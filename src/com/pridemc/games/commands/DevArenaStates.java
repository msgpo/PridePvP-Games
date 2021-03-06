package com.pridemc.games.commands;

import com.pridemc.games.arena.Arena;
import com.pridemc.games.arena.ArenaManager;
import com.pridemc.games.arena.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class DevArenaStates {

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		MessageUtil.sendMsg(sender, "Arenas:");

		for (Arena arena : ArenaManager.getArenas()) {
			String msg = "    " + ChatColor.AQUA + "%s" + ChatColor.YELLOW + ": %s" + ChatColor.YELLOW + ", Players[" + ChatColor.GOLD + "%d" + ChatColor.YELLOW + "/" + ChatColor.GOLD + "%d" + ChatColor.YELLOW + "], Votes[" + ChatColor.GOLD + "%d" + ChatColor.YELLOW + "/" + ChatColor.GOLD + "%d" + ChatColor.YELLOW + "], %sSetup";
			MessageUtil.sendMsgNoPrefix(sender, msg,
					arena.getName(),
					(arena.getState().canJoin() ? ChatColor.GREEN : ChatColor.RED) + arena.getState().getShortName(),
					arena.getNumPlayers(),
					arena.getMaxNumPlayers(),
					arena.getNumVotesToStart(),
					arena.getNumVotesRequiredToStart(),
					arena.isSetup() ? ChatColor.GREEN : ChatColor.RED);
		}

		return true;
	}
}
