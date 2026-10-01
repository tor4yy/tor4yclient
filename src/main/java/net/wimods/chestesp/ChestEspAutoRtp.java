/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Periodically sends a configurable command (e.g. "/rtp") and keeps track of
 * where each big, sudden position jump landed, so the quick menu can show a
 * simple map of visited random-teleport points.
 *
 * <p>
 * "The player got teleported" is detected as a big jump in position between
 * two ticks, rather than by reading any server-specific chat message. That
 * makes it work with whatever RTP command a given server uses, but it also
 * means any sufficiently large teleport (not just ones from the configured
 * command) will show up as a point.
 * </p>
 */
public final class ChestEspAutoRtp
{
	private static final double TELEPORT_THRESHOLD_SQ = 100.0 * 100.0;
	
	public record Point(double x, double z)
	{}
	
	private final List<Point> points = new ArrayList<>();
	private Vec3 lastPos;
	private int timer;
	
	public List<Point> getPoints()
	{
		return points;
	}
	
	public void clearPoints()
	{
		points.clear();
	}
	
	public void update(ChestEspConfig config)
	{
		if(!config.auto_rtp_enabled)
		{
			lastPos = null;
			timer = 0;
			return;
		}
		
		LocalPlayer player = Minecraft.getInstance().player;
		if(player == null)
			return;
		
		Vec3 pos = player.position();
		if(lastPos != null
			&& lastPos.distanceToSqr(pos) > TELEPORT_THRESHOLD_SQ)
			points.add(new Point(pos.x, pos.z));
		
		lastPos = pos;
		
		if(timer-- > 0)
			return;
		
		timer = Math.max(1, config.auto_rtp_interval_minutes) * 20 * 60;
		sendCommand(player, config.auto_rtp_command);
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
