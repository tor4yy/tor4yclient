/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import java.util.ArrayList;
import java.util.List;

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The mod's only settings screen, opened and closed with the menu keybind
 * (backslash by default). Everything is nested: the ESP section holds one
 * entry per category, and a category only shows its settings while it is
 * switched on.
 */
public final class ChestEspMenuScreen extends Screen
{
	private static final int PANEL_W = 208;
	private static final int ROW_H = 15;
	private static final int GAP = 1;
	private static final int PAD = 6;
	private static final int HEADER_H = 18;
	private static final int TOGGLE_W = 36;
	private static final int COLOR_W = 52;
	
	private static final int COL_PANEL = 0xF00E0E13;
	private static final int COL_BORDER = 0xFF2A2A33;
	private static final int COL_TITLE = 0xFFE8E8ED;
	private static final int COL_SCROLL = 0x66FFFFFF;
	
	private static final int MAP_SIZE = 100;
	private static final int MAP_MARGIN = 8;
	private static final int MAP_MIN_RANGE = 10_000;
	
	private static final int[] PALETTE = {0xFFFFFF, 0xC0C0C0, 0x808080,
		0xFF0000, 0xFF8000, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x0080FF, 0x0000FF,
		0x00008B, 0x8000FF, 0xFF00FF, 0xFF80C0};
	private static final String[] PALETTE_NAMES =
		{"White", "Silver", "Gray", "Red", "Orange", "Yellow", "Green", "Cyan",
			"Sky", "Blue", "Navy", "Purple", "Magenta", "Pink"};
	
	// Expand/collapse state is remembered while the game is running.
	private static boolean espExpanded = true;
	private static boolean blocksExpanded = false;
	private static boolean mobsExpanded = false;
	private static boolean playersExpanded = false;
	private static boolean oreExpanded = false;
	private static boolean spawnerFinderExpanded = false;
	private static boolean autoRtpExpanded = false;
	private static boolean autoSellExpanded = false;
	private static boolean autoBuyExpanded = false;
	private static boolean hotbarExpanded = false;
	
	private final List<RowBuilder> rows = new ArrayList<>();
	private int panelX, panelY, panelH;
	private int viewTop, viewBottom;
	private int contentH;
	private int scroll;
	
	@FunctionalInterface
	private interface RowBuilder
	{
		void build(int y);
	}
	
	@FunctionalInterface
	private interface StyleGetter
	{
		ChestEspStyle get(ChestEspConfig c);
	}
	
	@FunctionalInterface
	private interface StyleSetter
	{
		void set(ChestEspConfig c, ChestEspStyle style);
	}
	
	@FunctionalInterface
	private interface BoolSetter
	{
		void set(boolean value);
	}
	
	public ChestEspMenuScreen()
	{
		super(Component.literal("tor4yclient"));
	}
	
	@Override
	protected void init()
	{
		rows.clear();
		collectRows();
		
		contentH = rows.size() * (ROW_H + GAP);
		panelH = Math.min(height - 40, HEADER_H + PAD * 2 + contentH);
		panelX = (width - PANEL_W) / 2;
		panelY = (height - panelH) / 2;
		viewTop = panelY + HEADER_H;
		viewBottom = panelY + panelH - PAD;
		scroll = Mth.clamp(scroll, 0, maxScroll());
		
		int y = viewTop + PAD - scroll;
		for(RowBuilder row : rows)
		{
			if(y + ROW_H > viewTop && y < viewBottom)
				row.build(y);
			
			y += ROW_H + GAP;
		}
	}
	
	private int viewHeight()
	{
		return viewBottom - viewTop - PAD;
	}
	
	private int maxScroll()
	{
		return Math.max(0, contentH - viewHeight());
	}
	
	private void rebuild()
	{
		clearWidgets();
		init();
	}
	
	private void collectRows()
	{
		ChestEspMod mod = ChestEspMod.getInstance();
		ConfigHolder<ChestEspConfig> ch = mod.getConfigHolder();
		ChestEspGroupManager groups = mod.getGroups();
		
		addSectionRow("ESP", espExpanded, () -> espExpanded = !espExpanded);
		
		if(espExpanded)
		{
			boolean blocksOn = ch.get().include_block_entities;
			addCategoryRow("Block Entities", blocksExpanded,
				() -> blocksExpanded = !blocksExpanded, blocksOn, on -> {
					ch.get().include_block_entities = on;
					ch.save();
				});
			
			if(blocksOn && blocksExpanded)
			{
				addStyleRow(ch, c -> c.block_style,
					(c, style) -> c.block_style = style);
				
				for(ChestEspGroup group : groups.allGroups)
				{
					if(group == groups.mobs || group == groups.players
						|| groups.oreGroups.contains(group))
						continue;
					
					addGroupRow(group);
				}
			}
			
			boolean mobsOn = groups.mobs.isEnabled();
			addCategoryRow("Mobs", mobsExpanded,
				() -> mobsExpanded = !mobsExpanded, mobsOn,
				on -> groups.mobs.setEnabled(on));
			
			if(mobsOn && mobsExpanded)
			{
				addStyleRow(ch, c -> c.mob_style,
					(c, style) -> c.mob_style = style);
				addColorRow(groups.mobs, "Color");
			}
			
			boolean playersOn = groups.players.isEnabled();
			addCategoryRow("Players", playersExpanded,
				() -> playersExpanded = !playersExpanded, playersOn,
				on -> groups.players.setEnabled(on));
			
			if(playersOn && playersExpanded)
			{
				addStyleRow(ch, c -> c.player_style,
					(c, style) -> c.player_style = style);
				addColorRow(groups.players, "Color");
			}
			
			boolean oreOn = ch.get().include_ore_esp;
			addCategoryRow("Ore", oreExpanded, () -> oreExpanded = !oreExpanded,
				oreOn, on -> {
					ch.get().include_ore_esp = on;
					ch.save();
				});
			
			if(oreOn && oreExpanded)
			{
				addStyleRow(ch, c -> c.ore_style,
					(c, style) -> c.ore_style = style);
				
				for(ChestEspOreGroup ore : groups.oreGroups)
					addGroupRow(ore);
			}
		}
		
		boolean finderOn = ch.get().spawner_finder_enabled;
		addCategoryRow("Spawner Finder", spawnerFinderExpanded,
			() -> spawnerFinderExpanded = !spawnerFinderExpanded, finderOn,
			on -> {
				ch.get().spawner_finder_enabled = on;
				ch.save();
			});
		
		if(finderOn && spawnerFinderExpanded)
		{
			addSpawnerTargetRow(ch);
			
			List<ChestEspSpawnerFinder.Found> found =
				mod.getSpawnerFinder().getFound();
			if(found.isEmpty())
				addInfoRow("No spawners found nearby");
			else
				for(ChestEspSpawnerFinder.Found f : found)
					addInfoRow(String.format("%s: %d, %d, %d (%.0fm)",
						f.mobName(), f.pos().getX(), f.pos().getY(),
						f.pos().getZ(), f.distance()));
		}
		
		boolean rtpOn = ch.get().auto_rtp_enabled;
		addCategoryRow("Auto RTP", autoRtpExpanded,
			() -> autoRtpExpanded = !autoRtpExpanded, rtpOn, on -> {
				ch.get().auto_rtp_enabled = on;
				ch.save();
			});
		
		if(rtpOn && autoRtpExpanded)
		{
			addRtpCommandRow(ch);
			addRtpIntervalRow(ch);
		}
		
		boolean sellOn = ch.get().auto_sell_enabled;
		addCategoryRow("Auto Sell", autoSellExpanded,
			() -> autoSellExpanded = !autoSellExpanded, sellOn, on -> {
				ch.get().auto_sell_enabled = on;
				ch.save();
			});
		
		if(sellOn && autoSellExpanded)
		{
			addTextFieldRow("Sell command", () -> ch.get().auto_sell_command,
				value -> {
					ch.get().auto_sell_command = value;
					ch.save();
				});
			addTextFieldRow("Item name(s)", () -> ch.get().auto_sell_item_name,
				value -> {
					ch.get().auto_sell_item_name = value;
					ch.save();
				});
			addIntervalRow("Every", ch.get().auto_sell_interval_seconds,
				val -> {
					ch.get().auto_sell_interval_seconds = val;
					ch.save();
				});
		}
		
		boolean buyOn = ch.get().auto_buy_enabled;
		addCategoryRow("Auto Buy", autoBuyExpanded,
			() -> autoBuyExpanded = !autoBuyExpanded, buyOn, on -> {
				ch.get().auto_buy_enabled = on;
				ch.save();
			});
		
		if(buyOn && autoBuyExpanded)
		{
			addTextFieldRow("Buy command", () -> ch.get().auto_buy_command,
				value -> {
					ch.get().auto_buy_command = value;
					ch.save();
				});
			addIntervalRow("Every", ch.get().auto_buy_interval_seconds, val -> {
				ch.get().auto_buy_interval_seconds = val;
				ch.save();
			});
		}
		
		addSectionRow("Hotbar", hotbarExpanded,
			() -> hotbarExpanded = !hotbarExpanded);
		
		if(hotbarExpanded)
		{
			addToggleRow("Next Slot Hotkey", 1, ch.get().next_slot_enabled,
				on -> {
					ch.get().next_slot_enabled = on;
					ch.save();
				});
			addSlotRow(ch, "Min Slot", true);
			addSlotRow(ch, "Max Slot", false);
		}
		
		addToggleRow("Auto Sprint", 0, ch.get().include_auto_sprint, on -> {
			ch.get().include_auto_sprint = on;
			ch.save();
		});
	}
	
	// Top-level section header: full width, click to fold.
	private void addSectionRow(String label, boolean expanded, Runnable toggle)
	{
		rows.add(y -> addRenderableWidget(
			Button.builder(Component.literal(arrow(expanded) + " " + label)
				.withStyle(ChatFormatting.WHITE), btn -> {
					toggle.run();
					rebuild();
				}).bounds(panelX + PAD, y, PANEL_W - PAD * 2, ROW_H).build()));
	}
	
	// One category: name folds its settings, the button on the right
	// switches the whole category on or off.
	private void addCategoryRow(String label, boolean expanded, Runnable toggle,
		boolean on, BoolSetter setter)
	{
		int indent = PAD + 8;
		int labelW = PANEL_W - indent - PAD - TOGGLE_W - GAP;
		
		rows.add(y -> {
			String prefix = on ? arrow(expanded) + " " : "  ";
			addRenderableWidget(
				Button.builder(Component.literal(prefix + label), btn -> {
					if(on)
					{
						toggle.run();
						rebuild();
					}
				}).bounds(panelX + indent, y, labelW, ROW_H).build());
				
			addRenderableWidget(Button.builder(onOffLabel(on), btn -> {
				setter.set(!on);
				rebuild();
			}).bounds(panelX + indent + labelW + GAP, y, TOGGLE_W, ROW_H)
				.build());
		});
	}
	
	// Plain on/off row inside a section.
	private void addToggleRow(String label, int depth, boolean on,
		BoolSetter setter)
	{
		int indent = PAD + depth * 8;
		int labelW = PANEL_W - indent - PAD - TOGGLE_W - GAP;
		
		rows.add(y -> {
			addRenderableWidget(Button.builder(
				Component.literal(label).withStyle(ChatFormatting.GRAY),
				btn -> {
					setter.set(!on);
					rebuild();
				}).bounds(panelX + indent, y, labelW, ROW_H).build());
				
			addRenderableWidget(Button.builder(onOffLabel(on), btn -> {
				setter.set(!on);
				rebuild();
			}).bounds(panelX + indent + labelW + GAP, y, TOGGLE_W, ROW_H)
				.build());
		});
	}
	
	// One block-entity or ore type: toggle on the left, colour on the
	// right.
	private void addGroupRow(ChestEspGroup group)
	{
		int indent = PAD + 16;
		int labelW = PANEL_W - indent - PAD - COLOR_W - GAP;
		String label = displayName(group.getName());
		
		rows.add(y -> {
			boolean on = group.isEnabled();
			addRenderableWidget(Button
				.builder(Component.literal(label + ": ").append(onOffLabel(on)),
					btn -> {
						group.setEnabled(!on);
						rebuild();
					})
				.bounds(panelX + indent, y, labelW, ROW_H).build());
					
			addRenderableWidget(Button.builder(colorLabel(group), btn -> {
				group.setColorRgb(nextColor(group.getColorRgb()));
				rebuild();
			}).bounds(panelX + indent + labelW + GAP, y, COLOR_W, ROW_H)
				.build());
		});
	}
	
	private void addColorRow(ChestEspGroup group, String label)
	{
		int indent = PAD + 16;
		int labelW = PANEL_W - indent - PAD - COLOR_W - GAP;
		
		rows.add(y -> {
			addRenderableWidget(Button.builder(
				Component.literal(label).withStyle(ChatFormatting.GRAY),
				btn -> {
					group.setColorRgb(nextColor(group.getColorRgb()));
					rebuild();
				}).bounds(panelX + indent, y, labelW, ROW_H).build());
				
			addRenderableWidget(Button.builder(colorLabel(group), btn -> {
				group.setColorRgb(nextColor(group.getColorRgb()));
				rebuild();
			}).bounds(panelX + indent + labelW + GAP, y, COLOR_W, ROW_H)
				.build());
		});
	}
	
	private void addStyleRow(ConfigHolder<ChestEspConfig> ch,
		StyleGetter getter, StyleSetter setter)
	{
		int indent = PAD + 16;
		int width = PANEL_W - indent - PAD;
		
		rows.add(y -> {
			ChestEspStyle style = getter.get(ch.get());
			addRenderableWidget(Button.builder(Component.literal("Style: ")
				.append(Component.translatable(style.toString())
					.withStyle(ChatFormatting.AQUA)),
				btn -> {
					setter.set(ch.get(), nextStyle(style));
					ch.save();
					rebuild();
				}).bounds(panelX + indent, y, width, ROW_H).build());
		});
	}
	
	private void addSpawnerTargetRow(ConfigHolder<ChestEspConfig> ch)
	{
		int indent = PAD + 8;
		int width = PANEL_W - indent - PAD;
		
		rows.add(y -> {
			SpawnerTarget target = ch.get().spawner_finder_target;
			addRenderableWidget(Button.builder(
				Component.literal("Target: ").append(Component
					.literal(target.toString()).withStyle(ChatFormatting.AQUA)),
				btn -> {
					SpawnerTarget[] values = SpawnerTarget.values();
					ch.get().spawner_finder_target =
						values[(target.ordinal() + 1) % values.length];
					ch.save();
					rebuild();
				}).bounds(panelX + indent, y, width, ROW_H).build());
		});
	}
	
	// Free-text field for the RTP command. Saved on every keystroke via
	// EditBox's responder, so it survives the menu being rebuilt without
	// needing to keep the same widget instance across rebuilds.
	private void addRtpCommandRow(ConfigHolder<ChestEspConfig> ch)
	{
		int indent = PAD + 16;
		int width = PANEL_W - indent - PAD;
		
		rows.add(y -> {
			EditBox box = new EditBox(font, panelX + indent, y, width, ROW_H,
				Component.literal("RTP Command"));
			box.setMaxLength(64);
			box.setValue(ch.get().auto_rtp_command);
			box.setResponder(value -> {
				ch.get().auto_rtp_command = value;
				ch.save();
			});
			addRenderableWidget(box);
		});
	}
	
	private static final int[] RTP_INTERVALS = {1, 2, 3, 5, 10, 15, 20, 30, 60};
	
	private void addRtpIntervalRow(ConfigHolder<ChestEspConfig> ch)
	{
		int indent = PAD + 16;
		int width = PANEL_W - indent - PAD;
		
		rows.add(y -> {
			int value = ch.get().auto_rtp_interval_minutes;
			addRenderableWidget(Button.builder(
				Component.literal("Every: ").append(Component
					.literal(value + " min").withStyle(ChatFormatting.AQUA)),
				btn -> {
					ch.get().auto_rtp_interval_minutes = nextInterval(value);
					ch.save();
					rebuild();
				}).bounds(panelX + indent, y, width, ROW_H).build());
		});
	}
	
	private static int nextInterval(int current)
	{
		for(int i = 0; i < RTP_INTERVALS.length; i++)
			if(RTP_INTERVALS[i] == current)
				return RTP_INTERVALS[(i + 1) % RTP_INTERVALS.length];
			
		return RTP_INTERVALS[0];
	}
	
	@FunctionalInterface
	private interface TextSetter
	{
		void set(String value);
	}
	
	@FunctionalInterface
	private interface TextGetter
	{
		String get();
	}
	
	@FunctionalInterface
	private interface IntSetter
	{
		void set(int value);
	}
	
	// Free-text field, saved on every keystroke via EditBox's responder, so
	// it survives the menu being rebuilt without needing to keep the same
	// widget instance across rebuilds.
	private void addTextFieldRow(String hint, TextGetter getter,
		TextSetter setter)
	{
		int indent = PAD + 16;
		int width = PANEL_W - indent - PAD;
		
		rows.add(y -> {
			EditBox box = new EditBox(font, panelX + indent, y, width, ROW_H,
				Component.literal(hint));
			box.setMaxLength(96);
			box.setValue(getter.get());
			box.setResponder(setter::set);
			addRenderableWidget(box);
		});
	}
	
	private static final int[] TRADE_INTERVALS =
		{1, 2, 3, 5, 10, 15, 30, 60, 120, 300};
	
	private void addIntervalRow(String label, int value, IntSetter setter)
	{
		int indent = PAD + 16;
		int width = PANEL_W - indent - PAD;
		
		rows.add(y -> addRenderableWidget(Button.builder(
			Component.literal(label + ": ").append(
				Component.literal(value + "s").withStyle(ChatFormatting.AQUA)),
			btn -> {
				setter.set(nextTradeInterval(value));
				rebuild();
			}).bounds(panelX + indent, y, width, ROW_H).build()));
	}
	
	private static int nextTradeInterval(int current)
	{
		for(int i = 0; i < TRADE_INTERVALS.length; i++)
			if(TRADE_INTERVALS[i] == current)
				return TRADE_INTERVALS[(i + 1) % TRADE_INTERVALS.length];
			
		return TRADE_INTERVALS[0];
	}
	
	// A single non-interactive line, used for the spawner coordinate list.
	private void addInfoRow(String text)
	{
		int indent = PAD + 16;
		int width = PANEL_W - indent - PAD;
		
		rows.add(
			y -> addRenderableWidget(Button
				.builder(Component.literal(text).withStyle(ChatFormatting.GRAY),
					btn -> {})
				.bounds(panelX + indent, y, width, ROW_H).build()));
	}
	
	private void addSlotRow(ConfigHolder<ChestEspConfig> ch, String label,
		boolean min)
	{
		int indent = PAD + 8;
		int width = PANEL_W - indent - PAD;
		
		rows.add(y -> {
			int value = min ? ch.get().next_slot_min : ch.get().next_slot_max;
			addRenderableWidget(Button.builder(Component.literal(label + ": ")
				.append(Component.literal(String.valueOf(value))
					.withStyle(ChatFormatting.AQUA)),
				btn -> {
					int next = value >= 9 ? 1 : value + 1;
					if(min)
						ch.get().next_slot_min = next;
					else
						ch.get().next_slot_max = next;
					
					ch.save();
					rebuild();
				}).bounds(panelX + indent, y, width, ROW_H).build());
		});
	}
	
	private static Component onOffLabel(boolean on)
	{
		return Component.literal(on ? "ON" : "OFF")
			.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
	}
	
	private static Component colorLabel(ChestEspGroup group)
	{
		int rgb = group.getColorRgb();
		return Component.literal(colorName(rgb))
			.withStyle(style -> style.withColor(rgb));
	}
	
	private static String colorName(int rgb)
	{
		for(int i = 0; i < PALETTE.length; i++)
			if(PALETTE[i] == rgb)
				return PALETTE_NAMES[i];
			
		return String.format("#%06X", rgb);
	}
	
	private static int nextColor(int rgb)
	{
		for(int i = 0; i < PALETTE.length; i++)
			if(PALETTE[i] == rgb)
				return PALETTE[(i + 1) % PALETTE.length];
			
		return PALETTE[0];
	}
	
	private static ChestEspStyle nextStyle(ChestEspStyle style)
	{
		ChestEspStyle[] values = ChestEspStyle.values();
		return values[(style.ordinal() + 1) % values.length];
	}
	
	private static String arrow(boolean expanded)
	{
		return expanded ? "\u25BC" : "\u25B6";
	}
	
	private static String displayName(String internalName)
	{
		String[] parts = internalName.split("_");
		StringBuilder sb = new StringBuilder();
		for(String part : parts)
		{
			if(sb.length() > 0)
				sb.append(' ');
			
			sb.append(Character.toUpperCase(part.charAt(0)))
				.append(part.substring(1));
		}
		return sb.toString();
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX,
		double scrollY)
	{
		if(maxScroll() > 0)
		{
			scroll = Mth.clamp(scroll - (int)(scrollY * 12), 0, maxScroll());
			rebuild();
			return true;
		}
		
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY,
		float partialTick)
	{
		drawPanel(guiGraphics);
		
		guiGraphics.enableScissor(panelX, viewTop, panelX + PANEL_W,
			viewBottom);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.disableScissor();
		
		drawHeader(guiGraphics);
		drawScrollbar(guiGraphics);
		drawRtpMap(guiGraphics, mouseX, mouseY);
	}
	
	private void drawPanel(GuiGraphics guiGraphics)
	{
		int x2 = panelX + PANEL_W;
		int y2 = panelY + panelH;
		
		// A single-pixel border around a flat panel; no extra decoration.
		guiGraphics.fill(panelX, panelY, x2, y2, COL_BORDER);
		guiGraphics.fill(panelX + 1, panelY + 1, x2 - 1, y2 - 1, COL_PANEL);
	}
	
	private void drawHeader(GuiGraphics guiGraphics)
	{
		guiGraphics.drawString(font, "tor4yclient", panelX + PAD, panelY + 6,
			COL_TITLE, false);
	}
	
	private void drawScrollbar(GuiGraphics guiGraphics)
	{
		int max = maxScroll();
		if(max <= 0)
			return;
		
		int trackH = viewHeight();
		int barH = Math.max(16, trackH * trackH / contentH);
		int barY = viewTop + PAD + (trackH - barH) * scroll / max;
		int barX = panelX + PANEL_W - 4;
		
		guiGraphics.fill(barX, barY, barX + 2, barY + barH, COL_SCROLL);
	}
	
	// A small square map to the right of the main panel, shown whenever
	// Auto RTP is on. World origin (0,0) is always the exact center, split
	// into four quadrants by a crosshair line; each recorded teleport is a
	// dot, and hovering one shows its coordinates.
	private void drawRtpMap(GuiGraphics guiGraphics, int mouseX, int mouseY)
	{
		ChestEspMod mod = ChestEspMod.getInstance();
		if(!mod.getConfigHolder().get().auto_rtp_enabled)
			return;
		
		List<ChestEspAutoRtp.Point> points = mod.getAutoRtp().getPoints();
		
		int mapX = panelX + PANEL_W + MAP_MARGIN;
		int mapY = panelY;
		int x2 = mapX + MAP_SIZE;
		int y2 = mapY + MAP_SIZE;
		
		guiGraphics.fill(mapX, mapY, x2, y2, COL_BORDER);
		guiGraphics.fill(mapX + 1, mapY + 1, x2 - 1, y2 - 1, COL_PANEL);
		
		int cx = (mapX + x2) / 2;
		int cy = (mapY + y2) / 2;
		
		// Crosshair line through the world origin, dividing the map into the
		// four +/- quadrants.
		guiGraphics.fill(cx, mapY + 1, cx + 1, y2 - 1, COL_BORDER);
		guiGraphics.fill(mapX + 1, cy, x2 - 1, cy + 1, COL_BORDER);
		
		// Scale so the furthest recorded point still fits inside the box,
		// with a minimum range so one or two nearby points don't zoom in to
		// the point of uselessness.
		double maxDist = MAP_MIN_RANGE;
		for(ChestEspAutoRtp.Point p : points)
			maxDist =
				Math.max(maxDist, Math.max(Math.abs(p.x()), Math.abs(p.z())));
		
		double scale = (MAP_SIZE / 2.0 - 4) / maxDist;
		
		ChestEspAutoRtp.Point hovered = null;
		for(ChestEspAutoRtp.Point p : points)
		{
			int px = cx + (int)Math.round(p.x() * scale);
			int pz = cy + (int)Math.round(p.z() * scale);
			
			guiGraphics.fill(px - 1, pz - 1, px + 2, pz + 2, 0xFFFFFF55);
			
			if(Math.abs(mouseX - px) <= 3 && Math.abs(mouseY - pz) <= 3)
				hovered = p;
		}
		
		if(hovered != null)
		{
			// Drawn by hand with fill()/drawString() rather than
			// GuiGraphics#renderTooltip - that call crashed on some client
			// versions, and this uses nothing this screen doesn't already
			// rely on elsewhere (see drawHeader/drawPanel).
			String text = String.format("%.0f, %.0f", hovered.x(), hovered.z());
			int tw = font.width(text);
			int tx = mouseX + 6;
			int ty = mouseY - 8;
			
			guiGraphics.fill(tx - 2, ty - 2, tx + tw + 2, ty + 9, 0xF0000000);
			guiGraphics.drawString(font, text, tx, ty, 0xFFFFFFFF, false);
		}
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
