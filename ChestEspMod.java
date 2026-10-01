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
import java.util.List;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
	private final KeyMapping toggleKey;
	private final KeyMapping menuKey;
	private final KeyMapping nextSlotKey;
	
	private boolean enabled;
	
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
		
		groups.allGroups.forEach(ChestEspGroup::clear);
		
		ChunkUtils.getLoadedBlockEntities().forEach(
			be -> groups.blockGroups.forEach(group -> group.addIfMatches(be)));
		
		MC.level.entitiesForRendering().forEach(
			e -> groups.entityGroups.forEach(group -> group.addIfMatches(e)));
	}
	
	public ChestEspStyle getStyleFor(ChestEspGroup group)
	{
		ChestEspConfig c = configHolder.get();
		if(group == groups.mobs)
			return c.mob_style;
		if(group == groups.players)
			return c.player_style;
		
		return c.block_style;
	}
	
	public boolean shouldCancelViewBobbing()
	{
		if(!enabled)
			return false;
		
		return groups.allGroups.stream().filter(ChestEspGroup::isEnabled)
			.anyMatch(g -> getStyleFor(g).hasLines());
	}
	
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		groups.entityGroups.stream().filter(ChestEspGroup::isEnabled)
			.forEach(g -> g.updateBoxes(partialTicks));
		
		renderBoxes(matrixStack);
		renderTracers(matrixStack, partialTicks);
		renderMobNames(matrixStack, partialTicks);
	}
	
	private void renderBoxes(PoseStack matrixStack)
	{
		for(ChestEspGroup group : groups.allGroups)
		{
			if(!group.isEnabled() || !getStyleFor(group).hasBoxes())
				continue;
			
			List<AABB> boxes = group.getBoxes();
			int quadsColor = group.getColorI(0x40);
			int linesColor = group.getColorI(0x80);
			
			RenderUtils.drawSolidBoxes(matrixStack, boxes, quadsColor, false);
			RenderUtils.drawOutlinedBoxes(matrixStack, boxes, linesColor,
				false);
		}
	}
	
	private void renderTracers(PoseStack matrixStack, float partialTicks)
	{
		for(ChestEspGroup group : groups.allGroups)
		{
			if(!group.isEnabled() || !getStyleFor(group).hasLines())
				continue;
			
			List<AABB> boxes = group.getBoxes();
			List<Vec3> ends = boxes.stream().map(AABB::getCenter).toList();
			int color = group.getColorI(0x80);
			
			RenderUtils.drawTracers(matrixStack, partialTicks, ends, color,
				false);
		}
	}
	
	// Draws the mob's display name above its ESP box, in the same color as
	// the mob ESP.
	private void renderMobNames(PoseStack matrixStack, float partialTicks)
	{
		ChestEspEntityGroup mobs = groups.mobs;
		if(!mobs.isEnabled())
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
	
	public PlausibleAnalytics getPlausible()
	{
		return plausible;
	}
}
