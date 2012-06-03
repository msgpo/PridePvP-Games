package com.pridemc.games.arena;

import ca.xshade.bukkit.util.ConfigUtil;
import ca.xshade.bukkit.util.TaskInjector;
import com.pridemc.games.Core;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Author: Chris H (Zren / Shade)
 * Date: 6/2/12
 */
public class ArenaManager {
	private static ArenaManager instance = new ArenaManager();
	private Map<String, Arena> arenaMap = new HashMap<String, Arena>();
	private Map<String, String> playerToArenaMap = new HashMap<String, String>();

	public static ArenaManager getInstance() {
		return instance;
	}

	public static Arena getArena(String name) {
		return getInstance().arenaMap.get(name);
	}

	public static void addPlayerToArena(Player player, String arenaName) throws Exception {
		Arena arena = getArena(arenaName);
		addPlayerToArena(player, arena);
	}

	// Use this method for future proofing
	// Eg: a player joins two arenas at once somehow.
	public static void addPlayerToArena(Player player, Arena arena) throws Exception {
		// Validation
		if (getInstance().playerToArenaMap.containsKey(player.getName()))
			throw new Exception(String.format("%s already belongs to %s while trying to join %s.",
					player.getName(),
					getInstance().playerToArenaMap.get(player.getName()),
					arena.getName()));
		if (arena.getState() != Arena.State.WAITING_FOR_PLAYERS)
			throw new Exception(); // Don't need an error. Just don't let player warp through the portal.
		if (arena.isFull())
			throw new Exception(String.format("%s is already full", arena.getName()));


		//
		_addPlayerToArena(player, arena);


		// Reaction
		if (arena.isFull()) {
			// Arena is ready
			TaskInjector.schedule(new ArenaGraceTask(arena), 0);
			long delay = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES); //TODO: should be configurable
			TaskInjector.schedule(new ArenaStartGameTask(arena), delay);
		}
	}

	private static void _addPlayerToArena(Player player, Arena arena) {
		arena.addPlayer(new ArenaPlayer(player));
		getInstance().playerToArenaMap.put(player.getName(), arena.getName());
		Core.instance.getPlaying().put(player, arena.getName()); //TODO Legacy
	}

	public static void removePlayerFromArena(Player player, Arena arena) {
		arena.removePlayer(player.getName());
		getInstance().playerToArenaMap.remove(player.getName());
		Core.instance.getPlaying().remove(player); //TODO Legacy
	}

	public static Arena getArenaPlayerIsIn(String playerName) {
		String arenaName = getInstance().playerToArenaMap.get(playerName);
		return getArena(arenaName);
	}

	public static void setPlayerAsDead(String playerName) {
		Arena arena = ArenaManager.getArenaPlayerIsIn(playerName);
		arena.setPlayerAsDead(playerName);
	}

	//TODO: Move somewhere more specific.
	public static Location getGlobalSpawnPoint() {
		return ConfigUtil.getLocationFromVector(Core.config, "Spawn location", "Spawn world");
	}

	public static void checkEndGameConditions(Arena arena) {
		if (arena.getState() == Arena.State.RUNNING_GAME) {
			List<ArenaPlayer> alivePlayers = arena.getAlivePlayers();
			if (alivePlayers.size() > 1) {
				return;
			} else {
				// End of game
				endGame(arena);
			}

		}
	}

	private static void endGame(Arena arena) {
		List<ArenaPlayer> alivePlayers = arena.getAlivePlayers();
		Player winningPlayer = alivePlayers.get(0).getPlayer();
		Bukkit.broadcastMessage(String.format("%s won %s!", winningPlayer.getName(), arena.getName()));

		for (ArenaPlayer arenaPlayer : alivePlayers) {
			ArenaManager.cleanUpPlayer(arenaPlayer.getPlayer());
		}
	}

	public static Arena addArena(Arena arena) {
		return getInstance().arenaMap.put(arena.getName(), arena);
	}

	public static void cleanUpPlayer(Player player) {
		Arena arena = getArenaPlayerIsIn(player.getName());
		arena.setPlayerAsDead(player.getName());
		player.getInventory().clear();
		player.teleport(ArenaManager.getGlobalSpawnPoint());
	}

	public static void resetArena(String arenaName) {
		addArena(new Arena(arenaName));
		//TODO probably more cleanup.
	}
}