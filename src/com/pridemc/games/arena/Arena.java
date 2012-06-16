package com.pridemc.games.arena;

import ca.xshade.bukkit.util.ConfigUtil;
import ca.xshade.bukkit.util.TaskInjector;
import com.pridemc.games.Core;
import com.pridemc.games.portal.ArenaPortal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Author: Chris H (Zren / Shade)
 * Date: 6/2/12
 */
public class Arena {
	public enum State {
		WAITING_FOR_PLAYERS("Open",
				true, false, true, false),
		COUNTING_DOWN("Starting Soon",
				true, false, true, false),
		INITIAL_GRACE_PERIOD("Started|Grace",
				false, true, true, false),
		RUNNING_GAME("Running",
				false, true, false, true);

		private boolean canJoin, canEditBlocks, canChangeClass, canPvP;
		private String shortName;

		private State(String shortName, boolean canJoin, boolean canEditBlocks, boolean canChangeClass, boolean canPvP) {
			setShortName(shortName);
			this.canJoin = canJoin;
			this.canEditBlocks = canEditBlocks;
			this.canChangeClass = canChangeClass;
			this.canPvP = canPvP;
		}

		public boolean canJoin() {
			return canJoin;
		}

		public boolean canEditBlocks() {
			return canEditBlocks;
		}

		public boolean canChangeClass() {
			return canChangeClass;
		}

		public boolean canPvP() {
			return canPvP;
		}

		public boolean canDropItems() {
			// Player can't drop items when the arena is in a state to choose classes.
			return !canChangeClass();
		}

		/**
		 * Limits to 16 chars for use on a sign.
		 * @return
		 */
		private void setShortName(String shortName) {
			this.shortName = shortName;
		}

		public String getShortName() {
			return shortName;
		}
	}

	private String name;
	private Map<String, ArenaPlayer> arenaPlayerMap = new HashMap<String, ArenaPlayer>();
	private Set<ArenaPlayer> arenaPlayers = new HashSet<ArenaPlayer>();
	private State state = State.WAITING_FOR_PLAYERS;
	private Map<ArenaPlayer, Location> playerSpawnPoints = new HashMap<ArenaPlayer, Location>();
	public long startTime = System.currentTimeMillis();
	private TaskInjector taskInjector = TaskInjector.newInstance();
	public Set<ArenaPlayer> playersVotingToStart = new HashSet<ArenaPlayer>();
	public ArenaPortal portal = null;

	final int DEFAULT_MAX_PLAYERS = 15;
	final int DEFAULT_PLAYERS_TO_START = 8;
	final int DEFAULT_VOTES_TO_START = 2;

	public Arena(String name) {
		this.name = name;

		if(!Core.arenas.getKeys(false).contains(getName())){
			Core.arenas.createSection(getName());
		}
		Core.arenas.set(getName() + ".max players", getMaxNumPlayers());
		Core.arenas.set(getName() + ".playercount to start", getNumPlayersRequiredToStart());
		Core.arenas.set(getName() + ".votes to start", getNumVotesRequiredToStart());
		if (!Core.arenas.isSet(getName() + ".spawnpoint"))
			Core.arenas.createSection(getName() + ".spawnpoint");
		if (!Core.arenas.isSet(getName() + ".world"))
			Core.arenas.createSection(getName() + ".world");
		Core.arenas.set(getName() + ".region.min", getRegionMinimum());
		Core.arenas.set(getName() + ".region.max", getRegionMaximum());
		setPortalBlockLocation(getPortalBlockLocation());
		setState(State.WAITING_FOR_PLAYERS);
		ArenaConfig.saveArenaConfig();
	}

	public String getName() {
		return name;
	}

	public int getNumPlayers() {
		return getArenaPlayers().size();
	}

	public Set<ArenaPlayer> getArenaPlayers() {
		return arenaPlayers;
	}

	public State getState() {
		return state;
	}

	protected void setState(State state) {
		if (state.ordinal() < this.state.ordinal()) {
			Logger logger = Logger.getLogger(getClass().getName());
			logger.warning("-----------------------------------------------------------");
			logger.warning("-------------------       BUG REPORT     ------------------");
			logger.warning("-----------------------------------------------------------");
			logger.warning(String.format("Arena %s is trying to go back to an earlier gamestate. %s -> %s", getName(), state.name(), this.state.name()));
			logger.log(Level.WARNING, "Stack Trace Below", Thread.currentThread().getStackTrace());
			logger.warning("-----------------------------------------------------------");
		}

		this.state = state;

		//Persist it? all the other code looks at it. <- Bad reason.
		//TODO Remove this retarded node
		Core.arenas.set(getName() + ".status code", getState().ordinal());
	}

	protected void addPlayer(ArenaPlayer arenaPlayer) {
		arenaPlayers.add(arenaPlayer);
		arenaPlayerMap.put(arenaPlayer.getName(), arenaPlayer);
	}

	public ArenaPlayer getArenaPlayer(String playerName) {
		return arenaPlayerMap.get(playerName);
	}

	public void removePlayer(String playerName) {
		ArenaPlayer arenaPlayer = getArenaPlayer(playerName);
		if (arenaPlayer != null) {
			arenaPlayers.remove(arenaPlayer);
			arenaPlayerMap.remove(playerName);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Vector> getGameSpawnVectors() {
		List<Vector> vectors = new ArrayList<Vector>();
		List configList = Core.arenas.getList(getName() + ".gamepoints");
		if (configList != null) {
			//TODO: Fuck this unsafe casting

			vectors = (List<Vector>)configList;
		}
		return vectors;
	}

	public World getWorld() {
		String worldName = Core.arenas.getString(getName() + ".world");
		return Bukkit.getWorld(worldName);
	}

	public List<Location> getGameSpawnPoints() {
		World world = getWorld();
		List<Location> gameSpawnPoints = new ArrayList<Location>();
		for (Vector vector : getGameSpawnVectors()) {
			gameSpawnPoints.add(vector.toLocation(world));
		}
		return gameSpawnPoints;
	}

	public int getMaxNumPlayers() {
		return Core.arenas.getInt(getName() + ".max players", DEFAULT_MAX_PLAYERS);
	}

	public boolean isFull() {
		return arenaPlayers.size() >= getMaxNumPlayers();
	}

	public List<Player> getBukkitPlayers() {
		return ArenaUtil.asBukkitPlayerList(getArenaPlayers());
	}


	public void setPlayerSpawnPoints() {
		List<Location> spawnPoints = getGameSpawnPoints();
		List<ArenaPlayer> players = new ArrayList<ArenaPlayer>(getArenaPlayers());
		Collections.shuffle(spawnPoints);

		//TODO: Check that spawnPoints.size() > players.size();

		for (int i = 0; i < players.size(); i++) {
			playerSpawnPoints.put(players.get(i), spawnPoints.get(i % spawnPoints.size()));
		}
	}

	public void teleportAllToGameSpawnPoint() {
		for (ArenaPlayer arenaPlayer : getArenaPlayers()) {
			teleportToGameSpawnPoint(arenaPlayer);
		}
	}

	public void teleportToGameSpawnPoint(ArenaPlayer arenaPlayer) {
		arenaPlayer.getPlayer().teleport(playerSpawnPoints.get(arenaPlayer));
	}

	public Location getSpawnPoint() {
		return ConfigUtil.getLocationFromVector(Core.arenas, getName() + ".spawnpoint", getName() + ".world");
	}


	public void setPlayerAsDead(String playerName) {
		ArenaPlayer arenaPlayer = getArenaPlayer(playerName);
		arenaPlayers.remove(arenaPlayer);
		playersVotingToStart.remove(arenaPlayer);
	}

	public boolean voteToStart(String playerName) {
		ArenaPlayer arenaPlayer = getArenaPlayer(playerName);
		if (playersVotingToStart.contains(arenaPlayer))
			return false;

		playersVotingToStart.add(arenaPlayer);
		return true;
	}

	public int getNumVotesToStart() {
		return playersVotingToStart.size();
	}

	public int getNumVotesRequiredToStart() {
		return Core.arenas.getInt(getName() + ".votes to start", DEFAULT_VOTES_TO_START);
	}

	public int getNumVotesNeededToStart() {
		return Math.max(0, getNumVotesRequiredToStart() - getNumVotesToStart()); // limit lower bounds to 0.
	}

	public int getNumPlayersRequiredToStart() {
		return Core.arenas.getInt(getName() + ".playercount to start", DEFAULT_PLAYERS_TO_START);
	}

	public int getNumPlayersNeededToStart() {
		return Math.max(0, getNumPlayersRequiredToStart() - getNumPlayers()); // limit lower bounds to 0.
	}

	public Set<ArenaPlayer> getPlayersVotingToStart() {
		return playersVotingToStart;
	}

	public Vector getRegionMinimum() {
		return Core.arenas.getVector(getName() + ".region.min", new Vector());
	}

	public Vector getRegionMaximum() {
		return Core.arenas.getVector(getName() + ".region.max", new Vector());
	}

	public TaskInjector getTaskInjector() {
		return taskInjector;
	}

	public void startTaskFor(State state) {
		scheduleTaskFor(state, 0);
	}

	public void scheduleTaskFor(State state, long delay) {
		getTaskInjector().cancelAll();
		switch (state) {
			case COUNTING_DOWN:
				getTaskInjector().schedule(new ArenaCountdownTask(this), delay);
				break;

			case INITIAL_GRACE_PERIOD:
				getTaskInjector().schedule(new ArenaGraceTask(this), delay);
				break;

			case RUNNING_GAME:
				getTaskInjector().schedule(new ArenaStartGameTask(this), delay);
				break;

			default:
				break;
		}
	}

	public Location getPortalBlockLocation() {
		return ConfigUtil.getLocationFromVector(Core.arenas, getName() + ".portal.vector", getName() + ".portal.world");
	}

	public void setPortalBlockLocation(Location location) {
		if (location == null)
			return;

		Core.arenas.set(getName() + ".portal.world", location.getWorld().getName());
		Core.arenas.set(getName() + ".portal.vector", location.toVector());
		/*try {
			Core.arenas.save(new File(Core.instance.getDataFolder(), "arenas.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}*/

		Location portalBlockLocation = getPortalBlockLocation();
		loadPortal(portalBlockLocation);
	}

	public void loadPortal(Location location) {
		if (location == null)
			return;

		if (hasPortal()) {
			//TODO
		}

		this.portal = new ArenaPortal(location.getBlock());
	}

	public void update() {
		updatePortal();
	}


	public void updatePortal() {
		if (!hasPortal())
			return;

		portal.update(this);
	}

	public boolean hasPortal() {
		return portal != null;
	}

	public ArenaPortal getPortal() {
		return portal;
	}
}
