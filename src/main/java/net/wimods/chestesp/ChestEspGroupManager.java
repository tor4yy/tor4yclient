/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import java.util.List;
import java.util.stream.Stream;

import me.shedaniel.autoconfig.ConfigHolder;
import net.wimods.chestesp.groups.*;

public final class ChestEspGroupManager
{
	public final NormalChestsGroup normalChests;
	public final TrapChestsGroup trapChests;
	public final EnderChestsGroup enderChests;
	public final ChestCartsGroup chestCarts;
	public final ChestBoatsGroup chestBoats;
	public final BarrelsGroup barrels;
	public final PotsGroup pots;
	public final ShulkerBoxesGroup shulkerBoxes;
	public final HoppersGroup hoppers;
	public final HopperCartsGroup hopperCarts;
	public final DroppersGroup droppers;
	public final DispensersGroup dispensers;
	public final CraftersGroup crafters;
	public final FurnacesGroup furnaces;
	public final SpawnersGroup spawners;
	public final MobsGroup mobs;
	public final PlayersGroup players;
	public final CoalOreGroup coalOre;
	public final IronOreGroup ironOre;
	public final CopperOreGroup copperOre;
	public final GoldOreGroup goldOre;
	public final RedstoneOreGroup redstoneOre;
	public final LapisOreGroup lapisOre;
	public final DiamondOreGroup diamondOre;
	public final EmeraldOreGroup emeraldOre;
	public final AncientDebrisGroup ancientDebris;
	
	public final List<ChestEspBlockGroup> blockGroups;
	public final List<ChestEspEntityGroup> entityGroups;
	public final List<ChestEspOreGroup> oreGroups;
	public final List<ChestEspGroup> allGroups;
	
	public ChestEspGroupManager(ConfigHolder<ChestEspConfig> ch)
	{
		normalChests = new NormalChestsGroup(ch);
		trapChests = new TrapChestsGroup(ch);
		enderChests = new EnderChestsGroup(ch);
		chestCarts = new ChestCartsGroup(ch);
		chestBoats = new ChestBoatsGroup(ch);
		barrels = new BarrelsGroup(ch);
		pots = new PotsGroup(ch);
		shulkerBoxes = new ShulkerBoxesGroup(ch);
		hoppers = new HoppersGroup(ch);
		hopperCarts = new HopperCartsGroup(ch);
		droppers = new DroppersGroup(ch);
		dispensers = new DispensersGroup(ch);
		crafters = new CraftersGroup(ch);
		furnaces = new FurnacesGroup(ch);
		spawners = new SpawnersGroup(ch);
		mobs = new MobsGroup(ch);
		players = new PlayersGroup(ch);
		coalOre = new CoalOreGroup(ch);
		ironOre = new IronOreGroup(ch);
		copperOre = new CopperOreGroup(ch);
		goldOre = new GoldOreGroup(ch);
		redstoneOre = new RedstoneOreGroup(ch);
		lapisOre = new LapisOreGroup(ch);
		diamondOre = new DiamondOreGroup(ch);
		emeraldOre = new EmeraldOreGroup(ch);
		ancientDebris = new AncientDebrisGroup(ch);
		
		blockGroups = List.of(normalChests, trapChests, enderChests, barrels,
			pots, shulkerBoxes, hoppers, droppers, dispensers, crafters,
			furnaces, spawners);
		
		entityGroups =
			List.of(chestCarts, chestBoats, hopperCarts, mobs, players);
		
		oreGroups = List.of(coalOre, ironOre, copperOre, goldOre, redstoneOre,
			lapisOre, diamondOre, emeraldOre, ancientDebris);
		
		allGroups = Stream
			.concat(Stream.concat(blockGroups.stream(), entityGroups.stream()),
				oreGroups.stream())
			.toList();
	}
}
