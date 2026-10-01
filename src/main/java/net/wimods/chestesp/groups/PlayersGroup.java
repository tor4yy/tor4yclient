/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp.groups;

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.wimods.chestesp.ChestEspConfig;
import net.wimods.chestesp.ChestEspEntityGroup;

public final class PlayersGroup extends ChestEspEntityGroup
{
	private static final Minecraft MC = Minecraft.getInstance();
	
	public PlayersGroup(ConfigHolder<ChestEspConfig> ch)
	{
		super(ch, "player");
	}
	
	@Override
	protected boolean isEnabled(ChestEspConfig c)
	{
		return c.include_players;
	}
	
	@Override
	protected void setEnabled(ChestEspConfig c, boolean enabled)
	{
		c.include_players = enabled;
	}
	
	@Override
	protected int getColor(ChestEspConfig c)
	{
		return c.player_color;
	}
	
	@Override
	protected void setColor(ChestEspConfig c, int color)
	{
		c.player_color = color;
	}
	
	@Override
	public boolean matches(Entity e)
	{
		// Skip the local player, there's no need to highlight yourself.
		if(e == MC.player)
			return false;
		
		return e instanceof Player && e.isAlive();
	}
}
