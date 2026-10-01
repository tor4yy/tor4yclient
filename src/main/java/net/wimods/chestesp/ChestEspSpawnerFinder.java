/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.wimods.chestesp.util.ChunkUtils;

/**
 * Finds loaded monster spawners and lists their coordinates for the quick
 * menu, optionally filtered to one mob type. Purely informational: it only
 * reads block-entity data the client already has, and never places, breaks,
 * or interacts with anything.
 */
public final class ChestEspSpawnerFinder
{
	// Capped so a spawner-dense area doesn't turn the menu into an endless
	// list.
	private static final int MAX_RESULTS = 30;
	
	public record Found(BlockPos pos, String mobName, double distance)
	{}
	
	private List<Found> found = List.of();
	
	public List<Found> getFound()
	{
		return found;
	}
	
	public void update(ChestEspConfig config)
	{
		if(!config.spawner_finder_enabled)
		{
			if(!found.isEmpty())
				found = List.of();
			
			return;
		}
		
		LocalPlayer player = Minecraft.getInstance().player;
		if(player == null)
			return;
		
		List<Found> results = new ArrayList<>();
		ChunkUtils.getLoadedBlockEntities().forEach(be -> {
			if(!(be instanceof SpawnerBlockEntity spawner))
				return;
			
			String mobName = getMobName(spawner);
			if(!config.spawner_finder_target.matches(mobName))
				return;
			
			BlockPos pos = be.getBlockPos();
			double distance = Math.sqrt(player.distanceToSqr(pos.getX() + 0.5,
				pos.getY() + 0.5, pos.getZ() + 0.5));
			results.add(new Found(pos, mobName, distance));
		});
		
		results.sort(Comparator.comparingDouble(Found::distance));
		found = results.size() > MAX_RESULTS
			? new ArrayList<>(results.subList(0, MAX_RESULTS)) : results;
	}
	
	// Reads the mob type from the same preview-entity data vanilla already
	// uses to render the little spinning mob inside the spawner cage, so
	// this doesn't need anything the client wouldn't already have.
	private static String getMobName(SpawnerBlockEntity spawner)
	{
		try
		{
			Entity display = spawner.getSpawner().getOrCreateDisplayEntity(
				spawner.getLevel(), spawner.getBlockPos());
			if(display != null)
				return display.getType().getDescription().getString();
		}catch(Exception e)
		{
			// Some spawners may not have a resolvable preview entity yet
			// (e.g. right after the chunk loads); just report them as
			// unknown rather than failing the whole scan.
		}
		
		return "Unknown";
	}
}
