package com.pridemc.games.arena;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import uk.co.oliwali.HawkEye.HawkEye;
import uk.co.oliwali.HawkEye.PlayerSession;
import uk.co.oliwali.HawkEye.Rollback;
import uk.co.oliwali.HawkEye.SearchParser;
import uk.co.oliwali.HawkEye.callbacks.RollbackCallback;
import uk.co.oliwali.HawkEye.database.SearchQuery;
import uk.co.oliwali.HawkEye.util.HawkEyeAPI;

import java.text.SimpleDateFormat;
import java.util.Calendar;


/**
 * Author: Chris H (Zren / Shade)
 * Date: 6/3/12
 */
public class RevertManager {
	public static void revertArena(Arena arena) {

		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("HawkEye");
		HawkEye hawkEye = (HawkEye)plugin;

		CuboidRegion arenaRegion = arena.getRegion();
		if (arenaRegion == null)
			return;

		//Setup a SearchParser instance and set values
		SearchParser parser = new SearchParser();
		parser.minLoc = arenaRegion.getMin();
		parser.maxLoc = arenaRegion.getMax();
		parser.worlds = new String[]{arenaRegion.getWorld().getName()};
		//parser.actions = Arrays.asList(new DataType[]{DataType.BLOCK_BREAK, DataType.BLOCK_PLACE});

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(arena.getStartTime());
		parser.dateFrom = sdf.format(cal.getTime());

		parser.parseLocations();

		//Call search function
		HawkEyeAPI.performSearch(new RollbackCallback(new PlayerSession(Bukkit.getConsoleSender()), Rollback.RollbackType.GLOBAL), parser, SearchQuery.SearchDir.DESC);


		/*
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(arena.startTime);
		String dateFrom = sdf.format(cal.getTime());
		String command = String.format("hawk rollback t:%s",
				dateFrom,
				)
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
		*/
	}
}