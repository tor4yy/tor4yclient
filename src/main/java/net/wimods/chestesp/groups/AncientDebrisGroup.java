/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp.groups;

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.world.level.block.Blocks;
import net.wimods.chestesp.ChestEspConfig;
import net.wimods.chestesp.ChestEspOreGroup;

public final class AncientDebrisGroup extends ChestEspOreGroup
{
	public AncientDebrisGroup(ConfigHolder<ChestEspConfig> ch)
	{
		super(ch, "ancient_debris", 8, 119, Blocks.ANCIENT_DEBRIS);
	}
	
	@Override
	protected boolean isEnabled(ChestEspConfig c)
	{
		return c.include_ancient_debris;
	}
	
	@Override
	protected void setEnabled(ChestEspConfig c, boolean enabled)
	{
		c.include_ancient_debris = enabled;
	}
	
	@Override
	protected int getColor(ChestEspConfig c)
	{
		return c.ancient_debris_color;
	}
	
	@Override
	protected void setColor(ChestEspConfig c, int color)
	{
		c.ancient_debris_color = color;
	}
}
