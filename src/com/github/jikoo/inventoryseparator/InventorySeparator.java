package com.github.jikoo.inventoryseparator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Serialization-based inventory grouping for Bukkit. Per-gamemode, per-worldgroup.
 * 
 * @author Jikoo
 */
public class InventorySeparator extends JavaPlugin {

	private final HashMap<String, InventoryGroup> byWorld = new HashMap<>(), byName = new HashMap<>();

	@Override
	public void onEnable() {
		saveDefaultConfig();

		loadGroups();

		getServer().getPluginManager().registerEvents(new PlayerGameModeChangeListener(this), this);
		getServer().getPluginManager().registerEvents(new PlayerChangedWorldListener(this), this);
	}

	@Override
	public void onDisable() {}

	private void loadGroups() {
		if (getConfig().isConfigurationSection("groups")) {
			loadConfiguredGroups();
		} else {
			getLogger().warning("No group shares set in config.yml! Assuming separation per-world!");
		}

		for (World world : Bukkit.getWorlds()) {
			String worldName = world.getName().toLowerCase();
			if (byWorld.containsKey(worldName)) {
				continue;
			}
			// Unmanaged worlds
			getWorldGroup(worldName);
		}
	}

	private void loadConfiguredGroups() {
		final ConfigurationSection groups = getConfig().getConfigurationSection("groups");
		for (String groupName : groups.getKeys(false)) {
			if (!groups.isConfigurationSection(groupName)) {
				getLogger().warning("Malformed group " + groupName);
				continue;
			}

			final ConfigurationSection group = groups.getConfigurationSection(groupName);

			final List<String> worlds = getConfig().getStringList("worlds");
			if (worlds.isEmpty()) {
				getLogger().warning("Group " + groupName + " has no applicable worlds!");
				continue;
			}

			groupName = groupName.toLowerCase();

			final GameMode gamemode;
			if (!group.isString("gamemode")) {
				gamemode = getServer().getDefaultGameMode();
				getLogger().severe("Using server default gamemode of " + gamemode.name() + " for " + groupName);
			} else {
				String gamemodeName = group.getString("gamemode").toUpperCase();
				switch (gamemodeName) {
				case "SURVIVAL":
				case "CREATIVE":
				case "ADVENTURE":
				case "SPECTATOR":
					gamemode = GameMode.valueOf(gamemodeName);
					break;
				default:
					gamemode = getServer().getDefaultGameMode();
					getLogger().severe("Using server default gamemode of " + gamemode.name() + " for " + groupName);
					break;
				}
			}

			InventoryGroup invGroup = new InventoryGroup(this, groupName, gamemode);

			for (String worldName : worlds) {
				byWorld.put(worldName.toLowerCase(), invGroup);
			}
			byName.put(groupName, invGroup);
		}
	}

	public InventoryGroup getWorldGroup(String worldName) {
		worldName = worldName.toLowerCase();
		if (!byWorld.containsKey(worldName)) {
			final InventoryGroup group;
			if (byName.containsKey(worldName)) {
				getLogger().severe("The ungrouped world " + worldName + " shares a name with a group! It will be added to that group.");
				getLogger().severe("This is not advisable. Please fix your config!");
				group = byName.get(worldName);
			} else {
				getLogger().severe("Ungrouped world " + worldName + " found! Handling as a separate group.");
				group = new InventoryGroup(this, worldName, getServer().getDefaultGameMode());
				getLogger().severe("Using server default gamemode of " + group.getDefaultGameMode().name());
				byName.put(worldName, group);
			}
			byWorld.put(worldName, group);
		}
		return byWorld.get(worldName.toLowerCase());
	}

	public InventoryGroup getGroupByName(String groupName) {
		return byName.get(groupName.toLowerCase());
	}

	public HashSet<InventoryGroup> getAllGroups() {
		return new HashSet<InventoryGroup>(byWorld.values());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length < 1 || !args[0].equalsIgnoreCase("reload")) {
			return false;
		}
		reloadConfig();
		byWorld.clear();
		byName.clear();
		loadGroups();
		sender.sendMessage("Configuration reloaded!");
		return true;
	}
}
