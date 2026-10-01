/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

/**
 * The mob type the Spawner Finder is currently searching for. Matching is
 * done against the spawner's display name (the same text shown by vanilla's
 * own mob preview inside the spawner cage), so it's locale-dependent - it
 * expects the client to be running in English.
 */
public enum SpawnerTarget
{
	ANY("Any"),
	SKELETON("Skeleton"),
	ZOMBIE("Zombie"),
	SPIDER("Spider"),
	CAVE_SPIDER("Cave Spider"),
	BLAZE("Blaze"),
	SILVERFISH("Silverfish"),
	PIGLIN("Zombified Piglin");
	
	private final String label;
	
	SpawnerTarget(String label)
	{
		this.label = label;
	}
	
	public boolean matches(String mobName)
	{
		return this == ANY || label.equalsIgnoreCase(mobName);
	}
	
	@Override
	public String toString()
	{
		return label;
	}
}
