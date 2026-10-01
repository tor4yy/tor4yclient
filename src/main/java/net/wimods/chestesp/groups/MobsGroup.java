/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp.groups;

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.wimods.chestesp.ChestEspConfig;
import net.wimods.chestesp.ChestEspEntityGroup;

public final class MobsGroup extends ChestEspEntityGroup
{
	public MobsGroup(ConfigHolder<ChestEspConfig> ch)
	{
		super(ch, "mob");
	}
	
	@Override
	protected boolean isEnabled(ChestEspConfig c)
	{
		return c.include_mobs;
	}
	
	@Override
	protected void setEnabled(ChestEspConfig c, boolean enabled)
	{
		c.include_mobs = enabled;
	}
	
	@Override
	protected int getColor(ChestEspConfig c)
	{
		return c.mob_color;
	}
	
	@Override
	protected void setColor(ChestEspConfig c, int color)
	{
		c.mob_color = color;
	}
	
	@Override
	public boolean matches(Entity e)
	{
		return e instanceof Mob && e.isAlive();
	}
}
