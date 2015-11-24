package com.github.jikoo.inventoryseparator;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * A wrapper for managing player data for a group of worlds.
 * 
 * @author Jikoo
 */
public class InventoryGroup {

	private final InventorySeparator plugin;
	private final String name;
	private final GameMode gameMode;

	protected InventoryGroup(final InventorySeparator plugin, final String name, final GameMode gameMode) {
		this.plugin = plugin;
		this.name = name;
		this.gameMode = gameMode;
	}

	public String getName() {
		return name;
	}

	public GameMode getDefaultGameMode() {
		return gameMode;
	}

	private File getPlayerFile(final UUID uuid, final GameMode gameMode) {
		final File pluginFolder = plugin.getDataFolder();
		if (!pluginFolder.exists()) {
			pluginFolder.mkdirs();
		}
		final File dataFolder = new File(pluginFolder, "userdata");
		if (!dataFolder.exists()) {
			dataFolder.mkdir();
		}
		final File groupFolder = new File(dataFolder, getName());
		if (!groupFolder.exists()) {
			groupFolder.mkdir();
		}
		final File userFile = new File(groupFolder, uuid.toString() + "-" + gameMode.name() + ".yml");
		return userFile;
	}

	/**
	 * Change a player's inventory to another GameMode's inventory.
	 * N.B. This does not save their current inventory!
	 * 
	 * @param player the Player whose inventory is to be changed
	 * @param gameMode the GameMode to load inventory for
	 */
	public void changePlayerInventory(final Player player, final GameMode gameMode) {
		final PlayerInventory inv = player.getInventory();
		final File userFile = getPlayerFile(player.getUniqueId(), gameMode);
		if (!userFile.exists()) {
			inv.clear();
			inv.setArmorContents(new ItemStack[inv.getArmorContents().length]);
			return;
		}

		final YamlConfiguration config = YamlConfiguration.loadConfiguration(userFile);

		ItemStack[] contents = inv.getContents();
		for (int i = 0; i < contents.length; i++) {
			contents[i] = config.getItemStack("items." + i);
		}

		inv.setContents(contents);

		contents = inv.getArmorContents();
		for (int i = 0; i < contents.length; i++) {
			contents[i] = config.getItemStack("armor." + i);
		}
		inv.setArmorContents(contents);
	}

	public void savePlayerInventory(final Player player) {
		final PlayerInventory inv = player.getInventory();
		final YamlConfiguration config = new YamlConfiguration();

		ItemStack[] contents = inv.getContents();
		for (int i = 0; i < contents.length; i++) {
			try {
				config.set("items." + i, contents[i]);
			} catch (Exception e) {
				// If an exception is thrown, it's a Spigot serialization issue. Log it and move on.
				plugin.getLogger().severe(String.format("Unable to save data for %s's %s in slot %s",
						player.getName(), contents[i].getType(), i));
				e.printStackTrace();
			}
		}

		contents = inv.getArmorContents();
		for (int i = 0; i < contents.length; i++) {
			try {
				config.set("armor." + i, contents[i]);
			} catch (Exception e) {
				plugin.getLogger().severe(String.format("Unable to save data for %s's %s in slot %s",
						player.getName(), contents[i].getType(), i));
				e.printStackTrace();
			}
		}

		final File userFile = getPlayerFile(player.getUniqueId(), player.getGameMode());
		try {
			config.save(userFile);
		} catch (IOException e) {
			plugin.getLogger().severe("Unable to save user data to " + userFile.getPath());
			e.printStackTrace();
		}
	}
}
