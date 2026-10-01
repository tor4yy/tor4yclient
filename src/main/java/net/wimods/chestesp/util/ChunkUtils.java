/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp.util;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public enum ChunkUtils
{
	;
	
	private static final Minecraft MC = Minecraft.getInstance();
	
	public static Stream<BlockEntity> getLoadedBlockEntities()
	{
		return getLoadedChunks()
			.flatMap(chunk -> chunk.getBlockEntities().values().stream());
	}
	
	public static Stream<LevelChunk> getLoadedChunks()
	{
		int radius = Math.max(2, MC.options.getEffectiveRenderDistance()) + 3;
		int diameter = radius * 2 + 1;
		
		ChunkPos center = MC.player.chunkPosition();
		ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
		ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);
		
		Stream<LevelChunk> stream = Stream.<ChunkPos> iterate(min, pos -> {
			
			int x = pos.x;
			int z = pos.z;
			
			x++;
			
			if(x > max.x)
			{
				x = min.x;
				z++;
			}
			
			if(z > max.z)
				throw new IllegalStateException("Stream limit didn't work.");
			
			return new ChunkPos(x, z);
			
		}).limit(diameter * diameter).filter(c -> MC.level.hasChunk(c.x, c.z))
			.map(c -> MC.level.getChunk(c.x, c.z)).filter(Objects::nonNull);
		
		return stream;
	}
	
	/**
	 * Scans a single chunk, within the given Y range, for block states
	 * matching the given predicate, calling {@code consumer} once per
	 * matching block found.
	 *
	 * <p>
	 * Unlike block entities, plain blocks (ores included) aren't kept in a
	 * list Minecraft already has lying around, so finding them means
	 * checking every block position in the chunk. This only scans one
	 * chunk at a time so callers (see ChestEspMod) can spread the cost of a
	 * full-world scan over many ticks instead of doing it all at once,
	 * which would otherwise show up as a stutter.
	 * </p>
	 */
	public static void forEachMatchingBlockInChunk(LevelChunk chunk, int minY,
		int maxY, Predicate<BlockState> predicate,
		BiConsumer<BlockPos, BlockState> consumer)
	{
		minY = Math.max(minY, MC.level.getMinY());
		maxY = Math.min(maxY, MC.level.getMaxY());
		
		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();
		
		for(int x = 0; x < 16; x++)
			for(int z = 0; z < 16; z++)
				for(int y = minY; y < maxY; y++)
				{
					BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
					BlockState state = chunk.getBlockState(pos);
					if(!state.isAir() && predicate.test(state))
						consumer.accept(pos, state);
				}
	}
	
}
