/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Two independent, manually-enabled timers:
 *
 * <ul>
 * <li><b>Auto Sell</b>: every few seconds, if any item in the player's
 * inventory matches one of the configured names, sends the configured sell
 * command.</li>
 * <li><b>Auto Buy</b>: every configured interval, sends the configured buy
 * command. This only repeats a fixed command on a timer - it doesn't read
 * any shop or auction-house listing, so it can't react to what's actually
 * for sale, and it can't be used to snipe time-limited listings.</li>
 * </ul>
 */
public final class ChestEspAutoTrade
{
	private int sellTimer;
	private int buyTimer;
	
	public void update(ChestEspConfig config)
	{
		updateSell(config);
		updateBuy(config);
	}
	
	private void updateSell(ChestEspConfig config)
	{
		if(!config.auto_sell_enabled)
		{
			sellTimer = 0;
			return;
		}
		
		if(sellTimer-- > 0)
			return;
		
		sellTimer = Math.max(1, config.auto_sell_interval_seconds) * 20;
		
		LocalPlayer player = Minecraft.getInstance().player;
		if(player == null)
			return;
		
		if(!hasMatchingItem(player, config.auto_sell_item_name))
			return;
		
		sendCommand(player, config.auto_sell_command);
	}
	
	private void updateBuy(ChestEspConfig config)
	{
		if(!config.auto_buy_enabled)
		{
			buyTimer = 0;
			return;
		}
		
		if(buyTimer-- > 0)
			return;
		
		buyTimer = Math.max(1, config.auto_buy_interval_seconds) * 20;
		
		LocalPlayer player = Minecraft.getInstance().player;
		if(player == null)
			return;
		
		sendCommand(player, config.auto_buy_command);
	}
	
	// Matches by display name, comma-separated so more than one item can be
	// watched for at once (e.g. "cobblestone, rotten flesh").
	private static boolean hasMatchingItem(LocalPlayer player, String query)
	{
		if(query == null || query.isBlank())
			return false;
		
		String[] names = query.toLowerCase().split(",");
		
		for(int i = 0; i < player.getInventory().getContainerSize(); i++)
		{
			ItemStack stack = player.getInventory().getItem(i);
			
			if(stack.isEmpty())
				continue;
			
			String name = stack.getHoverName().getString().toLowerCase();
			
			for(String n : names)
			{
				if(!n.isBlank() && name.contains(n.trim()))
					return true;
			}
		}
		
		return false;
	}
	
	private static void sendCommand(LocalPlayer player, String command)
	{
		if(command == null || command.isBlank())
			return;
		
		String cmd = command.trim();
		
		if(cmd.startsWith("/"))
			cmd = cmd.substring(1);
		
		player.connection.sendCommand(cmd);
	}
}
