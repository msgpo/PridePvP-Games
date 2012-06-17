package com.pridemc.games.arena;

import com.pridemc.games.classes.PlayerClass;
import com.pridemc.games.classes.PlayerClassManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Author: Chris H (Zren / Shade)
 * Date: 6/2/12
 */
public class ArenaPlayer {

	String name;

	public ArenaPlayer(Player player) {
		this.name = player.getName();
	}

	public ArenaPlayer(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Player getPlayer() {
		return Bukkit.getPlayer(getName());
	}

	public Arena getArena() {
		return ArenaManager.getArenaPlayerIsIn(getName());
	}

	@Override
	public boolean equals(Object obj) {
		return getName().equals(obj);
	}
	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	public boolean hasAClass() {
		return PlayerClassManager.hasAClass(getName());
	}

	public PlayerClass getPlayerClass() {
		return PlayerClassManager.getPlayerClass(getName());
	}

	public void equip() {
		if (hasAClass()) {
			Player player = getPlayer();

			// Reset player's inventory and effects.
			PlayerClass.resetPlayer(player);

			// Equip the player with select equipment
			if (getArena().getState().canUnpackEquipment())
				getPlayerClass().equipPlayer(player);
		}
	}
}
