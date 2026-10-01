/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.EnumHandler.EnumDisplayOption;

@Config(name = "chestesp")
public final class ChestEspConfig implements ConfigData
{
	@ConfigEntry.Gui.Tooltip
	public boolean enable = true;
	
	@ConfigEntry.Gui.EnumHandler(option = EnumDisplayOption.BUTTON)
	@ConfigEntry.Gui.Tooltip
	public ChestEspStyle block_style = ChestEspStyle.BOXES;
	
	@ConfigEntry.Gui.EnumHandler(option = EnumDisplayOption.BUTTON)
	@ConfigEntry.Gui.Tooltip
	public ChestEspStyle mob_style = ChestEspStyle.BOXES;
	
	@ConfigEntry.Gui.EnumHandler(option = EnumDisplayOption.BUTTON)
	@ConfigEntry.Gui.Tooltip
	public ChestEspStyle player_style = ChestEspStyle.BOXES;
	
	// Master switch for the whole block-entity ESP category.
	public boolean include_block_entities = true;
	
	public boolean include_basic_chests = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int chest_color = 0x00FF00;
	
	public boolean include_trap_chests = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int trap_chest_color = 0xFF8000;
	
	public boolean include_ender_chests = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int ender_chest_color = 0x00FFFF;
	
	public boolean include_chest_carts = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int chest_cart_color = 0xFFFF00;
	
	public boolean include_chest_boats = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int chest_boat_color = 0xFFFF00;
	
	public boolean include_barrels = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int barrel_color = 0x00FF00;
	
	public boolean include_pots = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int pot_color = 0x00FF00;
	
	public boolean include_shulker_boxes = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int shulker_box_color = 0xFF00FF;
	
	public boolean include_hoppers = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int hopper_color = 0xFFFFFF;
	
	public boolean include_hopper_carts = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int hopper_cart_color = 0xFFFF00;
	
	public boolean include_droppers = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int dropper_color = 0xFFFFFF;
	
	public boolean include_dispensers = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int dispenser_color = 0xFF8000;
	
	public boolean include_crafters = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int crafter_color = 0xFFFFFF;
	
	public boolean include_furnaces = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int furnace_color = 0xFF0000;
	
	public boolean include_spawners = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int spawner_color = 0xFF0000;
	
	public boolean include_mobs = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int mob_color = 0x00008B;
	
	public boolean include_players = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int player_color = 0xFFFFFF;
	
	@ConfigEntry.Gui.Tooltip
	public boolean next_slot_enabled = true;
	
	@ConfigEntry.Gui.Tooltip
	public boolean include_auto_sprint = false;
	
	@ConfigEntry.BoundedDiscrete(min = 1, max = 9)
	@ConfigEntry.Gui.Tooltip
	public int next_slot_min = 1;
	
	@ConfigEntry.BoundedDiscrete(min = 1, max = 9)
	@ConfigEntry.Gui.Tooltip
	public int next_slot_max = 9;
	
	// Master switch for the ore ESP category (see ChunkUtils for the
	// periodic world scan this drives).
	public boolean include_ore_esp = false;
	
	@ConfigEntry.Gui.EnumHandler(option = EnumDisplayOption.BUTTON)
	@ConfigEntry.Gui.Tooltip
	public ChestEspStyle ore_style = ChestEspStyle.BOXES;
	
	public boolean include_coal_ore = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int coal_ore_color = 0x2B2B2B;
	
	public boolean include_iron_ore = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int iron_ore_color = 0xD8AF93;
	
	public boolean include_copper_ore = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int copper_ore_color = 0xC26A45;
	
	public boolean include_gold_ore = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int gold_ore_color = 0xFFD700;
	
	public boolean include_redstone_ore = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int redstone_ore_color = 0xFF0000;
	
	public boolean include_lapis_ore = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int lapis_ore_color = 0x1E5FAE;
	
	public boolean include_diamond_ore = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int diamond_ore_color = 0x00FFFF;
	
	public boolean include_emerald_ore = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int emerald_ore_color = 0x00FF7F;
	
	public boolean include_ancient_debris = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int ancient_debris_color = 0x8B4726;
	
	// Spawner Finder: informational only - lists coordinates of loaded
	// spawners, optionally filtered to one mob type. See
	// ChestEspSpawnerFinder.
	public boolean spawner_finder_enabled = false;
	
	public SpawnerTarget spawner_finder_target = SpawnerTarget.ANY;
	
	// Auto RTP: periodically sends a command (e.g. "/rtp") and tracks
	// where each resulting teleport landed. See ChestEspAutoRtp.
	public boolean auto_rtp_enabled = false;
	
	public String auto_rtp_command = "/rtp";
	
	public int auto_rtp_interval_minutes = 5;
	
	@ConfigEntry.Gui.Tooltip
	public boolean plausible = true;
}
