/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Lightweight in-game quick menu, opened/closed with the ChestESP menu
 * keybind (backslash by default). Independent from the full ModMenu /
 * Cloth Config settings screen.
 */
public final class ChestEspMenuScreen extends Screen
{
	private static final int WIDTH = 180;
	private static final int ROW_HEIGHT = 20;
	private static final int PADDING = 4;
	
	// Remembers expand/collapse state across re-opens of the menu.
	private static boolean stylesExpanded = false;
	private static boolean blockEntitiesExpanded = false;
	private static boolean nextSlotExpanded = false;
	
	public ChestEspMenuScreen()
	{
		super(Component.literal("tor4yclient"));
	}
	
	@Override
	protected void init()
	{
		ChestEspMod mod = ChestEspMod.getInstance();
		ConfigHolder<ChestEspConfig> ch = mod.getConfigHolder();
		ChestEspGroupManager groups = mod.getGroups();
		
		int x = PADDING;
		int y = PADDING;
		
		y = addStylesSection(ch, x, y);
		y = addBlockEntitiesSection(groups, x, y);
		y = addToggleRow(groups.mobs, "Mobs ESP", x, y);
		y = addToggleRow(groups.players, "Players ESP", x, y);
		addNextSlotSection(ch, x, y);
	}
	
	// Collapsible section: enable/disable the next-slot hotkey, plus the
	// slot range (1-9) it cycles through.
	private int addNextSlotSection(ConfigHolder<ChestEspConfig> ch, int x,
		int y)
	{
		addRenderableWidget(Button.builder(nextSlotHeaderLabel(), btn -> {
			nextSlotExpanded = !nextSlotExpanded;
			refreshWidgets();
		}).bounds(x, y, WIDTH, ROW_HEIGHT).build());
		y += ROW_HEIGHT + PADDING;
		
		if(!nextSlotExpanded)
			return y;
		
		y = addBoolToggleRow(ch, "Enabled", x + PADDING, y,
			WIDTH - PADDING * 2, c -> c.next_slot_enabled,
			(c, enabled) -> c.next_slot_enabled = enabled);
		y = addIntCycleRow(ch, "Min Slot", x + PADDING, y,
			WIDTH - PADDING * 2, c -> c.next_slot_min,
			(c, val) -> c.next_slot_min = val);
		y = addIntCycleRow(ch, "Max Slot", x + PADDING, y,
			WIDTH - PADDING * 2, c -> c.next_slot_max,
			(c, val) -> c.next_slot_max = val);
		
		return y + PADDING;
	}
	
	private Component nextSlotHeaderLabel()
	{
		String arrow = nextSlotExpanded ? "\u25BC" : "\u25B6";
		return Component.literal(arrow + " Next Slot Hotkey");
	}
	
	private interface IntGetter
	{
		int get(ChestEspConfig c);
	}
	
	private interface IntSetter
	{
		void set(ChestEspConfig c, int value);
	}
	
	private static final Integer[] SLOT_NUMBERS =
		{1, 2, 3, 4, 5, 6, 7, 8, 9};
	
	private int addIntCycleRow(ConfigHolder<ChestEspConfig> ch, String label,
		int x, int y, int width, IntGetter getter, IntSetter setter)
	{
		addRenderableWidget(CycleButton
			.<Integer> builder(val -> Component.literal(String.valueOf(val)),
				getter.get(ch.get()))
			.withValues(SLOT_NUMBERS)
			.create(x, y, width, ROW_HEIGHT, Component.literal(label),
				(btn, val) -> {
					setter.set(ch.get(), val);
					ch.save();
				}));
		
		return y + ROW_HEIGHT + 2;
	}
	
	private interface BoolGetter
	{
		boolean get(ChestEspConfig c);
	}
	
	private interface BoolSetter
	{
		void set(ChestEspConfig c, boolean enabled);
	}
	
	private int addBoolToggleRow(ConfigHolder<ChestEspConfig> ch, String label,
		int x, int y, int width, BoolGetter getter, BoolSetter setter)
	{
		addRenderableWidget(
			CycleButton.onOffBuilder(getter.get(ch.get())).create(x, y, width,
				ROW_HEIGHT, Component.literal(label), (btn, enabled) -> {
					setter.set(ch.get(), enabled);
					ch.save();
				}));
		
		return y + ROW_HEIGHT + 2;
	}
	
	// Collapsible section with one style (boxes/lines/both) per ESP
	// category: block entities, mobs, and players.
	private int addStylesSection(ConfigHolder<ChestEspConfig> ch, int x, int y)
	{
		addRenderableWidget(Button.builder(stylesHeaderLabel(), btn -> {
			stylesExpanded = !stylesExpanded;
			refreshWidgets();
		}).bounds(x, y, WIDTH, ROW_HEIGHT).build());
		y += ROW_HEIGHT + PADDING;
		
		if(!stylesExpanded)
			return y;
		
		y = addStyleRow(ch, "Block Entities", x + PADDING, y,
			WIDTH - PADDING * 2, c -> c.block_style,
			(c, style) -> c.block_style = style);
		y = addStyleRow(ch, "Mobs", x + PADDING, y, WIDTH - PADDING * 2,
			c -> c.mob_style, (c, style) -> c.mob_style = style);
		y = addStyleRow(ch, "Players", x + PADDING, y, WIDTH - PADDING * 2,
			c -> c.player_style, (c, style) -> c.player_style = style);
		
		return y + PADDING;
	}
	
	private interface StyleGetter
	{
		ChestEspStyle get(ChestEspConfig c);
	}
	
	private interface StyleSetter
	{
		void set(ChestEspConfig c, ChestEspStyle style);
	}
	
	private int addStyleRow(ConfigHolder<ChestEspConfig> ch, String label,
		int x, int y, int width, StyleGetter getter, StyleSetter setter)
	{
		addRenderableWidget(CycleButton
			.<ChestEspStyle> builder(
				style -> Component.translatable(style.toString()),
				getter.get(ch.get()))
			.withValues(ChestEspStyle.values()).create(x, y, width, ROW_HEIGHT,
				Component.literal(label), (btn, style) -> {
					setter.set(ch.get(), style);
					ch.save();
				}));
		
		return y + ROW_HEIGHT + 2;
	}
	
	private int addBlockEntitiesSection(ChestEspGroupManager groups, int x,
		int y)
	{
		addRenderableWidget(Button.builder(blockEntitiesHeaderLabel(), btn -> {
			blockEntitiesExpanded = !blockEntitiesExpanded;
			refreshWidgets();
		}).bounds(x, y, WIDTH, ROW_HEIGHT).build());
		y += ROW_HEIGHT + PADDING;
		
		if(!blockEntitiesExpanded)
			return y;
		
		for(ChestEspGroup group : groups.allGroups)
		{
			// Mobs and players get their own toggles, separate from this
			// category.
			if(group == groups.mobs || group == groups.players)
				continue;
			
			y = addToggleRow(group, displayName(group.getName()), x + PADDING,
				y, WIDTH - PADDING * 2);
		}
		
		return y + PADDING;
	}
	
	private int addToggleRow(ChestEspGroup group, String label, int x, int y)
	{
		return addToggleRow(group, label, x, y, WIDTH);
	}
	
	private int addToggleRow(ChestEspGroup group, String label, int x, int y,
		int width)
	{
		addRenderableWidget(CycleButton.onOffBuilder(group.isEnabled()).create(
			x, y, width, ROW_HEIGHT, Component.literal(label),
			(btn, enabled) -> group.setEnabled(enabled)));
		
		return y + ROW_HEIGHT + 2;
	}
	
	private void refreshWidgets()
	{
		clearWidgets();
		init();
	}
	
	private Component stylesHeaderLabel()
	{
		String arrow = stylesExpanded ? "\u25BC" : "\u25B6";
		return Component.literal(arrow + " Styles ESP");
	}
	
	private Component blockEntitiesHeaderLabel()
	{
		String arrow = blockEntitiesExpanded ? "\u25BC" : "\u25B6";
		return Component.literal(arrow + " ESP Block Entities");
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
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY,
		float partialTick)
	{
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
