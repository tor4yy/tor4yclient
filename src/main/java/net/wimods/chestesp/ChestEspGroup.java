/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.world.phys.AABB;

public abstract class ChestEspGroup
{
	private final ConfigHolder<ChestEspConfig> configHolder;
	private final String name;
	protected final ArrayList<AABB> boxes = new ArrayList<>();
	
	public ChestEspGroup(ConfigHolder<ChestEspConfig> configHolder, String name)
	{
		this.configHolder = Objects.requireNonNull(configHolder);
		this.name = Objects.requireNonNull(name);
	}
	
	protected abstract boolean isEnabled(ChestEspConfig c);
	
	protected abstract void setEnabled(ChestEspConfig c, boolean enabled);
	
	protected abstract int getColor(ChestEspConfig c);
	
	protected abstract void setColor(ChestEspConfig c, int color);
	
	public void clear()
	{
		boxes.clear();
	}
	
	public final String getName()
	{
		return name;
	}
	
	public final boolean isEnabled()
	{
		return isEnabled(configHolder.get());
	}
	
	public final void setEnabled(boolean enabled)
	{
		ChestEspConfig c = configHolder.get();
		if(isEnabled(c) == enabled)
			return;
		
		setEnabled(c, enabled);
		configHolder.save();
	}
	
	public final int getColorI(int alpha)
	{
		int rgb = getColor(configHolder.get());
		return alpha << 24 | rgb;
	}
	
	public final int getColorRgb()
	{
		return getColor(configHolder.get());
	}
	
	public final void setColorRgb(int color)
	{
		ChestEspConfig c = configHolder.get();
		if(getColor(c) == color)
			return;
		
		setColor(c, color);
		configHolder.save();
	}
	
	public final String getColorHex()
	{
		int rgb = getColor(configHolder.get());
		return String.format("#%06X", rgb);
	}
	
	public final List<AABB> getBoxes()
	{
		return Collections.unmodifiableList(boxes);
	}
}
