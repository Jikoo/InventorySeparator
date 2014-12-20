package com.github.jikoo.inventoryseparator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

	private static InventorySeparator instance;
	private HashMap<String, InventoryGroup> byWorld;
	private HashMap<String, InventoryGroup> byName;

	public void onEnable() {
		byWorld = new HashMap<>();
		byName = new HashMap<>();
		instance = this;
		saveDefaultConfig();

		loadGroups();

		getServer().getPluginManager().registerEvents(new PlayerGameModeChangeListener(), this);
	}

	public void onDisable() {
		instance = null;
	}

	public static InventorySeparator getInstance() {
		return instance;
	}

	public void loadGroups() {
		final ConfigurationSection groups = getConfig().getConfigurationSection("groups");
		if (groups == null) {
			getLogger().warning("No group shares set in config.yml! Assuming separation per-world!");
		} else {
			for (String groupName : getConfig().getConfigurationSection("groups").getKeys(false)) {
				groupName = groupName.toLowerCase();
				try {
					InventoryGroup group = new InventoryGroup(groupName,
							GameMode.valueOf(getConfig().getString("groups." + groupName + ".gamemode", "SURVIVAL")));
					List<String> worlds = getConfig().getStringList("groups." + groupName + ".worlds");
					boolean hasWorlds = false;
					for (String worldName : worlds) {
						World world = Bukkit.getWorld(worldName);
						if (world == null) {
							continue;
						}
						hasWorlds = true;
						byWorld.put(worldName.toLowerCase(), group);
					}
					if (!hasWorlds) {
						getLogger().warning("Group " + groupName + " has no applicable worlds!");
						continue;
					}
					byName.put(groupName, group);
				} catch (Exception e) {
					// Generic catch-all to stop config mistakes preventing load
					getLogger().severe("Malformed group " + groupName);
					e.printStackTrace();
				}
			}
		}
		// Unmanaged worlds
		for (World world : Bukkit.getWorlds()) {
			String worldName = world.getName().toLowerCase();
			if (byWorld.containsKey(worldName)) {
				continue;
			}
			InventoryGroup group;
			if (byName.containsKey(worldName)) {
				getLogger().severe("The ungrouped world " + worldName + " shares a name with a group! It will be added to that group.");
				getLogger().severe("This is not advisable. Please fix your config!");
				// TODO might be better to have a different format instead, worldName + "-world" or something
				group = byName.get(worldName);
			} else {
				getLogger().severe("Ungrouped world " + worldName + " found! Handling as a separate group.");
				group = new InventoryGroup(worldName, getServer().getDefaultGameMode());
				getLogger().severe("Using server default gamemode of " + group.getDefaultGameMode().name());
			}
			byName.put(worldName, group);
			byWorld.put(worldName, group);
		}
	}

	public InventoryGroup getWorldGroup(String worldName) {
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
		// TODO
		sender.sendMessage(ChatColor.RED + "This isn't a thing yet.");
		return true;
	}
}
