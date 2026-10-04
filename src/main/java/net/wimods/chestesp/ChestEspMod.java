/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wimods.chestesp.groups.MobsGroup;
import net.wimods.chestesp.groups.PlayersGroup;
import net.wimods.chestesp.util.ChunkUtils;
import net.wimods.chestesp.util.EntityUtils;
import net.wimods.chestesp.util.PlausibleAnalytics;
import net.wimods.chestesp.util.RenderUtils;
import org.lwjgl.glfw.GLFW;

public final class ChestEspMod
{
	private static final Minecraft MC = Minecraft.getInstance();
	public static final Logger LOGGER = LoggerFactory.getLogger("tor4yclient");
	
	private final ConfigHolder<ChestEspConfig> configHolder;
	private final PlausibleAnalytics plausible;
	private final ChestEspGroupManager groups;
	private final ChestEspSpawnerFinder spawnerFinder =
		new ChestEspSpawnerFinder();
	private final ChestEspAutoRtp autoRtp = new ChestEspAutoRtp();
	private final ChestEspAutoTrade autoTrade = new ChestEspAutoTrade();
	private final KeyMapping toggleKey;
	private final KeyMapping menuKey;
	private final KeyMapping nextSlotKey;
	
	private boolean enabled;
	
	// Ore ESP doesn't have a ready-made list of ore positions like
	// chests/mobs do, so it scans loaded chunks itself. Doing a whole
	// world's worth of chunks in one go would show up as a stutter, so
	// this queue lets it process a few chunks per tick instead, cycling
	// through all loaded chunks over several seconds.
	private static final int ORE_CHUNKS_PER_TICK = 2;
	private final Deque<LevelChunk> oreScanQueue = new ArrayDeque<>();
	
	// Players get a thinner outline than other ESP categories, and
	// whichever player is currently under the crosshair is highlighted in
	// this color instead of the configured player color.
	private static final int PLAYER_TARGET_COLOR_RGB = 0xFF0000;
	private static final float PLAYER_LINE_WIDTH = 1;
	
	public ChestEspMod()
	{
		LOGGER.info("Starting tor4yclient...");
		
		configHolder = AutoConfig.register(ChestEspConfig.class,
			GsonConfigSerializer::new);
		
		groups = new ChestEspGroupManager(configHolder);
		
		KeyMapping.Category kbCategory = KeyMapping.Category
			.register(Identifier.fromNamespaceAndPath("chestesp", "chestesp"));
		toggleKey = KeyBindingHelper
			.registerKeyBinding(new KeyMapping("key.chestesp.toggle",
				InputConstants.UNKNOWN.getValue(), kbCategory));
		menuKey =
			KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.chestesp.menu", InputConstants.Type.KEYSYM
					.getOrCreate(GLFW.GLFW_KEY_BACKSLASH).getValue(),
				kbCategory));
		nextSlotKey = KeyBindingHelper
			.registerKeyBinding(new KeyMapping("key.chestesp.next_slot",
				InputConstants.UNKNOWN.getValue(), kbCategory));
		
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean enabled = configHolder.get().enable;
			while(toggleKey.consumeClick())
				setEnabled(!enabled);
			
			while(menuKey.consumeClick())
			{
				if(MC.screen instanceof ChestEspMenuScreen)
					MC.setScreen(null);
				else if(MC.screen == null)
					MC.setScreen(new ChestEspMenuScreen());
			}
			
			while(nextSlotKey.consumeClick())
				selectNextHotbarSlot();
			
			updateAutoSprint();
			autoRtp.update(configHolder.get());
			autoTrade.update(configHolder.get());
		});
		
		plausible = new PlausibleAnalytics(configHolder, groups, toggleKey);
		plausible.pageview("/");
	}
	
	public void setEnabled(boolean enabled)
	{
		if(this.enabled == enabled)
			return;
		
		LOGGER.info("{} tor4yclient.", enabled ? "Enabling" : "Disabling");
		
		this.enabled = enabled;
		
		if(!enabled)
			groups.allGroups.forEach(ChestEspGroup::clear);
		
		if(configHolder.get().enable != enabled)
		{
			configHolder.get().enable = enabled;
			configHolder.save();
		}
	}
	
	public void onUpdate()
	{
		setEnabled(configHolder.get().enable);
		if(!isEnabled())
			return;
		
		groups.blockGroups.forEach(ChestEspGroup::clear);
		groups.entityGroups.forEach(ChestEspGroup::clear);
		
		if(configHolder.get().include_block_entities)
			ChunkUtils.getLoadedBlockEntities().forEach(
				be -> groups.blockGroups.forEach(g -> g.addIfMatches(be)));
		
		MC.level.entitiesForRendering().forEach(
			e -> groups.entityGroups.forEach(group -> group.addIfMatches(e)));
		
		updateOres();
		spawnerFinder.update(configHolder.get());
	}
	
	private void updateOres()
	{
		if(!configHolder.get().include_ore_esp)
		{
			if(!oreScanQueue.isEmpty())
				oreScanQueue.clear();
			
			groups.oreGroups.forEach(ChestEspGroup::clear);
			return;
		}
		
		List<ChestEspOreGroup> activeOres =
			groups.oreGroups.stream().filter(ChestEspGroup::isEnabled).toList();
		if(activeOres.isEmpty())
		{
			oreScanQueue.clear();
			groups.oreGroups.forEach(ChestEspGroup::clear);
			return;
		}
		
		// Once the current sweep of the loaded chunks finishes, start a new
		// one. The old boxes are only cleared here, right as the new sweep
		// begins, so ore ESP doesn't flicker away and back while it's
		// gradually refreshed.
		if(oreScanQueue.isEmpty())
		{
			groups.oreGroups.forEach(ChestEspGroup::clear);
			ChunkUtils.getLoadedChunks().forEach(oreScanQueue::add);
		}
		
		int minY =
			activeOres.stream().mapToInt(ore -> ore.minY).min().orElseThrow();
		int maxY =
			activeOres.stream().mapToInt(ore -> ore.maxY).max().orElseThrow();
		Predicate<BlockState> isAnyOre =
			state -> activeOres.stream().anyMatch(ore -> ore.matches(state));
		
		for(int i = 0; i < ORE_CHUNKS_PER_TICK && !oreScanQueue.isEmpty(); i++)
		{
			LevelChunk chunk = oreScanQueue.poll();
			ChunkUtils.forEachMatchingBlockInChunk(chunk, minY, maxY, isAnyOre,
				(pos, state) -> {
					for(ChestEspOreGroup ore : activeOres)
						if(ore.matches(state))
						{
							ore.addBlock(pos);
							break;
						}
				});
		}
	}
	
	public ChestEspStyle getStyleFor(ChestEspGroup group)
	{
		ChestEspConfig c = configHolder.get();
		if(group == groups.mobs)
			return c.mob_style;
		if(group == groups.players)
			return c.player_style;
		if(group instanceof ChestEspOreGroup)
			return c.ore_style;
		
		return c.block_style;
	}
	
	// Whether the ESP category a group belongs to is switched on. Mobs and
	// players are their own categories; everything else sits under the
	// block-entity master switch.
	public boolean isCategoryEnabled(ChestEspGroup group)
	{
		ChestEspConfig c = configHolder.get();
		if(group == groups.mobs)
			return c.include_mobs;
		if(group == groups.players)
			return c.include_players;
		if(group instanceof ChestEspOreGroup)
			return c.include_ore_esp;
		
		return c.include_block_entities;
	}
	
	private boolean isGroupActive(ChestEspGroup group)
	{
		return group.isEnabled() && isCategoryEnabled(group);
	}
	
	public boolean shouldCancelViewBobbing()
	{
		if(!enabled)
			return false;
		
		return groups.allGroups.stream().filter(this::isGroupActive)
			.anyMatch(g -> getStyleFor(g).hasLines());
	}
	
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		groups.entityGroups.stream().filter(this::isGroupActive)
			.forEach(g -> g.updateBoxes(partialTicks));
		
		renderBoxes(matrixStack);
		renderTracers(matrixStack, partialTicks);
		renderMobNames(matrixStack, partialTicks);
	}
	
	private void renderBoxes(PoseStack matrixStack)
	{
		for(ChestEspGroup group : groups.allGroups)
		{
			if(!isGroupActive(group) || !getStyleFor(group).hasBoxes())
				continue;
			
			if(group == groups.players)
			{
				renderPlayerBoxes(matrixStack);
				continue;
			}
			
			List<AABB> boxes = group.getBoxes();
			int quadsColor = group.getColorI(0x40);
			int linesColor = group.getColorI(0x80);
			
			RenderUtils.drawSolidBoxes(matrixStack, boxes, quadsColor, false);
			RenderUtils.drawOutlinedBoxes(matrixStack, boxes, linesColor,
				false);
		}
	}
	
	// Splits the players group's boxes into "targeted" (under the
	// crosshair) and "everyone else", so the targeted one can be drawn in
	// red while the rest use the normal player color. The box already
	// matches the player's exact hitbox (see EntityUtils.getLerpedBox).
	private void renderPlayerBoxes(PoseStack matrixStack)
	{
		PlayersGroup players = groups.players;
		List<Entity> entities = players.getEntities();
		List<AABB> boxes = players.getBoxes();
		Entity targeted = MC.crosshairPickEntity;
		
		List<AABB> normalBoxes = new ArrayList<>();
		List<AABB> targetBoxes = new ArrayList<>();
		for(int i = 0; i < entities.size() && i < boxes.size(); i++)
		{
			if(entities.get(i) == targeted)
				targetBoxes.add(boxes.get(i));
			else
				normalBoxes.add(boxes.get(i));
		}
		
		int normalQuads = players.getColorI(0x40);
		int normalLines = players.getColorI(0x80);
		int targetQuads = 0x40 << 24 | PLAYER_TARGET_COLOR_RGB;
		int targetLines = 0x80 << 24 | PLAYER_TARGET_COLOR_RGB;
		
		RenderUtils.drawSolidBoxes(matrixStack, normalBoxes, normalQuads,
			false);
		RenderUtils.drawOutlinedBoxes(matrixStack, normalBoxes, normalLines,
			false, PLAYER_LINE_WIDTH);
		
		RenderUtils.drawSolidBoxes(matrixStack, targetBoxes, targetQuads,
			false);
		RenderUtils.drawOutlinedBoxes(matrixStack, targetBoxes, targetLines,
			false, PLAYER_LINE_WIDTH);
	}
	
	private void renderTracers(PoseStack matrixStack, float partialTicks)
	{
		for(ChestEspGroup group : groups.allGroups)
		{
			if(!isGroupActive(group) || !getStyleFor(group).hasLines())
				continue;
			
			if(group == groups.players)
			{
				renderPlayerTracers(matrixStack, partialTicks);
				continue;
			}
			
			List<AABB> boxes = group.getBoxes();
			List<Vec3> ends = boxes.stream().map(AABB::getCenter).toList();
			int color = group.getColorI(0x80);
			
			RenderUtils.drawTracers(matrixStack, partialTicks, ends, color,
				false);
		}
	}
	
	// Same targeted/everyone-else split as renderPlayerBoxes, but for the
	// tracer lines.
	private void renderPlayerTracers(PoseStack matrixStack, float partialTicks)
	{
		PlayersGroup players = groups.players;
		List<Entity> entities = players.getEntities();
		List<AABB> boxes = players.getBoxes();
		Entity targeted = MC.crosshairPickEntity;
		
		List<Vec3> normalEnds = new ArrayList<>();
		List<Vec3> targetEnds = new ArrayList<>();
		for(int i = 0; i < entities.size() && i < boxes.size(); i++)
		{
			Vec3 center = boxes.get(i).getCenter();
			if(entities.get(i) == targeted)
				targetEnds.add(center);
			else
				normalEnds.add(center);
		}
		
		int normalColor = players.getColorI(0x80);
		int targetColor = 0x80 << 24 | PLAYER_TARGET_COLOR_RGB;
		
		RenderUtils.drawTracers(matrixStack, partialTicks, normalEnds,
			normalColor, false, PLAYER_LINE_WIDTH);
		RenderUtils.drawTracers(matrixStack, partialTicks, targetEnds,
			targetColor, false, PLAYER_LINE_WIDTH);
	}
	
	// Draws the mob's display name above its ESP box, in the same color as
	// the mob ESP.
	private void renderMobNames(PoseStack matrixStack, float partialTicks)
	{
		MobsGroup mobs = groups.mobs;
		if(!isGroupActive(mobs))
			return;
		
		int color = mobs.getColorI(0xFF);
		for(Entity e : mobs.getEntities())
		{
			Vec3 pos = EntityUtils.getLerpedPos(e, partialTicks).add(0,
				e.getBbHeight() + 0.5, 0);
			RenderUtils.drawText(matrixStack, pos,
				e.getDisplayName().getString(), color);
		}
	}
	
	public static ChestEspMod getInstance()
	{
		return ChestEspModInitializer.getInstance();
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public ConfigHolder<ChestEspConfig> getConfigHolder()
	{
		return configHolder;
	}
	
	public ChestEspGroupManager getGroups()
	{
		return groups;
	}
	
	public ChestEspSpawnerFinder getSpawnerFinder()
	{
		return spawnerFinder;
	}
	
	public ChestEspAutoRtp getAutoRtp()
	{
		return autoRtp;
	}
	
	public ChestEspAutoTrade getAutoTrade()
	{
		return autoTrade;
	}
	
	// Manually cycles to the next hotbar slot within the configured range
	// (next_slot_min - next_slot_max, 1-indexed as shown on screen),
	// wrapping back to the start. This is a plain hotkey - it only runs
	// when the player presses it and is not triggered by any game event.
	private void selectNextHotbarSlot()
	{
		ChestEspConfig c = configHolder.get();
		if(!c.next_slot_enabled)
			return;
		
		LocalPlayer player = MC.player;
		if(player == null)
			return;
			
		// Convert the 1-9 (as shown on screen) config range to 0-8
		// (internal slot indices), and make sure min <= max no matter which
		// order the user set them in.
		int min = Math.min(c.next_slot_min, c.next_slot_max) - 1;
		int max = Math.max(c.next_slot_min, c.next_slot_max) - 1;
		
		int current = player.getInventory().getSelectedSlot();
		int next;
		if(current < min || current >= max)
			next = min;
		else
			next = current + 1;
		
		player.getInventory().setSelectedSlot(next);
		player.connection.send(new ServerboundSetCarriedItemPacket(next));
	}
	
	// Simple movement QoL: sprints automatically while walking forward,
	// same as vanilla double-tap-W would, so the player doesn't have to.
	// Doesn't touch aiming, targeting or combat in any way.
	private void updateAutoSprint()
	{
		if(!configHolder.get().include_auto_sprint)
			return;
		
		LocalPlayer player = MC.player;
		if(player == null)
			return;
		
		boolean canSprint = player.getAbilities().mayfly
			|| player.getFoodData().getFoodLevel() > 6;
		boolean wantsSprint = MC.options.keyUp.isDown() && !player.isCrouching()
			&& !player.isUsingItem();
		
		if(wantsSprint && canSprint && !player.isSprinting())
			player.setSprinting(true);
		else if((!wantsSprint || !canSprint) && player.isSprinting())
			player.setSprinting(false);
	}
	
	public PlausibleAnalytics getPlausible()
	{
		return plausible;
	}
}
