/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import me.shedaniel.autoconfig.ConfigHolder;

/**
 * A group of ore ESP entries that all match one or more plain block types
 * (e.g. a stone and a deepslate variant of the same ore). Unlike chests and
 * mobs, ore blocks aren't ticking objects Minecraft keeps a list of, so
 * {@link ChestEspMod} finds them with a periodic world scan (see
 * {@link net.wimods.chestesp.util.ChunkUtils#forEachMatchingBlock}) instead
 * of the usual per-tick update.
 */
public abstract class ChestEspOreGroup extends ChestEspGroup
{
	private final Block[] blocks;
	
	// The world-height range this ore can actually generate in, used to
	// skip scanning heights where it could never appear. A bit of slack is
	// fine here; it only has to not be narrower than the real range.
	public final int minY;
	public final int maxY;
	
	public ChestEspOreGroup(ConfigHolder<ChestEspConfig> ch, String name,
		int minY, int maxY, Block... blocks)
	{
		super(ch, name);
		this.minY = minY;
		this.maxY = maxY;
		this.blocks = blocks;
	}
	
	public final boolean matches(BlockState state)
	{
		for(Block block : blocks)
			if(state.is(block))
				return true;
			
		return false;
	}
	
	public final void addBlock(BlockPos pos)
	{
		boxes.add(new AABB(pos));
	}
}
