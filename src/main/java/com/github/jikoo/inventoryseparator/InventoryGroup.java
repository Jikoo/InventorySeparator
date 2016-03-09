package com.github.jikoo.inventoryseparator;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

		this.setContents(inv, contents);

		contents = inv.getArmorContents();
		for (int i = 0; i < contents.length; i++) {
			contents[i] = config.getItemStack("armor." + i);
		}
		inv.setArmorContents(contents);

		try {
			inv.setItemInOffHand(config.getItemStack("offhand"));
		} catch (NoSuchMethodError e) {
			// 1.8 or lower
		}

		player.setHealth(config.getDouble("health", 20));
		player.setFoodLevel(config.getInt("food", 20));
		// TODO: Will this work? Should I just get and cast?
		player.setSaturation((float) config.getDouble("saturation", 20));
		player.setLevel(config.getInt("level", 0));
		// TODO: see above, functional, necessary?
		player.setExp((float) config.getDouble("levelProgress", 0));
		player.setFireTicks(config.getInt("fireTicks", 0));
		// TODO another float
		player.setFallDistance((float) config.getDouble("fallDistance", 0));

		for (PotionEffect effect : player.getActivePotionEffects()) {
			// This is safe - the Collection of potion effects is a copy.
			// It's also mutable, if it comes down to it.
			player.removePotionEffect(effect.getType());
		}

		final ConfigurationSection potions = config.createSection("potions");
		for (String effectName : config.getKeys(false)) {
			final PotionEffectType type = PotionEffectType.getByName(effectName);
			if (type == null) {
				continue;
			}
			final PotionEffect potion = new PotionEffect(type,
					potions.getInt(effectName + ".duration"),
					potions.getInt(effectName + ".amplifier"));
			player.addPotionEffect(potion, true);
		}
	}

	public void savePlayerInventory(final Player player) {
		final PlayerInventory inv = player.getInventory();
		final YamlConfiguration config = new YamlConfiguration();

		ItemStack[] contents = this.getContents(inv);
		for (int i = 0; i < contents.length; i++) {
			try {
				config.set("items." + i, contents[i]);
			} catch (Exception e) {
				// If an exception is thrown, it's probably a Spigot serialization issue. Log it and move on.
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

		try {
			config.set("offhand", inv.getItemInOffHand());
		} catch (NoSuchMethodError e) {
			// 1.8 or lower
		} catch (Exception e) {
			// If any other exception is thrown, it's probably a Spigot serialization issue. Log it and move on.
			plugin.getLogger().severe(String.format("Unable to save data for %s's offhand slot",
					player.getName()));
			e.printStackTrace();
		}

		config.set("health", player.getHealth());
		config.set("food", player.getFoodLevel());
		// TODO: Does this work? Is the cast needed?
		config.set("saturation", (double) player.getSaturation());
		config.set("level", player.getLevel());
		// TODO see above
		config.set("levelProgress", (double) player.getExp());
		config.set("fireTicks", player.getFireTicks());
		// TODO another float
		config.set("fallDistance", player.getFallDistance());

		final ConfigurationSection potions = config.createSection("potions");
		for (PotionEffect effect : player.getActivePotionEffects()) {
			final String type = effect.getType().getName();
			potions.set(type + ".duration", effect.getDuration());
			potions.set(type + ".amplifier", effect.getAmplifier());
		}

		final File userFile = getPlayerFile(player.getUniqueId(), player.getGameMode());
		try {
			config.save(userFile);
		} catch (IOException e) {
			plugin.getLogger().severe("Unable to save user data to " + userFile.getPath());
			e.printStackTrace();
		}
	}

	private ItemStack[] getContents(PlayerInventory inventory) {
		try {
			return inventory.getStorageContents();
		} catch (NoSuchMethodError e) {
			return inventory.getContents();
		}
	}

	private void setContents(PlayerInventory inventory, ItemStack[] contents) {
		try {
			inventory.setStorageContents(contents);
		} catch (NoSuchMethodError e) {
			inventory.setContents(contents);
		}
	}
}
