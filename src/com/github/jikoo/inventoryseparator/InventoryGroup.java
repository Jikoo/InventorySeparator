package com.github.jikoo.inventoryseparator;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * A wrapper for managing player data for a group of worlds.
 * 
 * @author Jikoo
 */
public class InventoryGroup {

	private final String name;
	private final GameMode gameMode;

	public InventoryGroup(final String name, final GameMode gameMode) {
		this.name = name;
		this.gameMode = gameMode;
	}

	public String getName() {
		return name;
	}

	public GameMode getDefaultGameMode() {
		return gameMode;
	}

	public File getPlayerData(final UUID uuid, final GameMode gameMode) {
		final File pluginFolder = InventorySeparator.getInstance().getDataFolder();
		if (!pluginFolder.exists()) {
			pluginFolder.mkdirs();
		}
		final File dataFolder = new File(pluginFolder, "userdata");
		if (!dataFolder.exists()) {
			dataFolder.mkdir();
		}
		final File userFile = new File(pluginFolder, uuid.toString() + "-" + gameMode.name() + ".yml");
		if (!userFile.exists()) {
			try {
				userFile.createNewFile();
			} catch (IOException e) {
				InventorySeparator.getInstance().getLogger().severe("Unable to create user data file " + userFile.getPath());
				e.printStackTrace();
			}
		}
		return userFile;
	}

	/**
	 * Change a player's inventory to another GameMode's inventory.
	 * N.B. This does not save their current inventory!
	 * 
	 * @param player the Player whose inventory is to be changed
	 * @param gameMode the GameMode to load inventory for
	 */
	public void changePlayerInventory(Player player, GameMode gameMode) {
		// TODO
	}

	public void savePlayerInventory(Player player) {
		// TODO
	}
}
