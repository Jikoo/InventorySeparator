package com.github.jikoo.inventoryseparator;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.Bukkit;
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

	private File getGroupFile() {
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
		return groupFolder;
	}

	private File getGenericFile(final UUID uuid) {
		return new File(getGroupFile(), uuid.toString() + ".yml");
	}

	private File getPlayerFile(final UUID uuid, final GameMode gameMode) {
		return new File(getGroupFile(), uuid.toString() + "-" + gameMode.name() + ".yml");
	}

	public GameMode getLastGameMode(UUID uuid) {

		final Player player = Bukkit.getPlayer(uuid);
		if (player != null && plugin.getWorldGroup(player.getWorld().getName()).equals(this)) {
			return player.getGameMode();
		}

		final File playerFile = getGenericFile(uuid);

		if (!playerFile.exists()) {
			return this.getDefaultGameMode();
		}

		final YamlConfiguration config = YamlConfiguration.loadConfiguration(getGenericFile(uuid));

		final String gameMode = config.getString("lastGameMode");

		if (gameMode == null) {
			return this.getDefaultGameMode();
		}

		try {
			return GameMode.valueOf(gameMode);
		} catch (IllegalArgumentException e) {
			return this.getDefaultGameMode();
		}
	}

	/**
	 * Change a player's inventory to another GameMode's inventory.
	 * N.B. This does not save their current inventory!
	 *
	 * @param player the Player whose inventory is to be changed
	 * @param gameMode the GameMode to load inventory for
	 */
	public void changePlayerInventory(final Player player, final GameMode gameMode) {

		if (player.isOnline()) {
			// TODO: If another plugin has loaded a Player, attempting to set potions will cause a NPE
			for (PotionEffect effect : player.getActivePotionEffects()) {
				// This is safe - the Collection of potion effects is a copy.
				player.removePotionEffect(effect.getType());
			}
		}
		final PlayerInventory inv = player.getInventory();
		final File userFile = getPlayerFile(player.getUniqueId(), gameMode);
		final YamlConfiguration config = YamlConfiguration.loadConfiguration(userFile);

		ItemStack[] contents = inv.getContents();
		for (int i = 0; i < contents.length; i++) {
			contents[i] = config.getItemStack("items." + i);
		}

		inv.setContents(contents);

		contents = player.getEnderChest().getContents();
		for (int i = 0; i < contents.length; i++) {
			contents[i] = config.getItemStack("enderchest." + i);
		}
		player.getEnderChest().setContents(contents);

		player.setHealth(Math.max(0, Math.min(20, config.getDouble("health", 20))));
		player.setFoodLevel(Math.max(0, Math.min(20, config.getInt("food", 20))));
		player.setSaturation((float) Math.max(0, Math.min(20, config.getDouble("saturation", 20))));
		player.setLevel(Math.max(0, config.getInt("level", 0)));
		player.setExp((float) Math.max(0, Math.min(1, config.getDouble("levelProgress", 0))));
		player.setFireTicks(Math.max(0, config.getInt("fireTicks", 0)));
		player.setFallDistance((float) Math.max(0, config.getDouble("fallDistance", 0)));

		if (!player.isOnline()) {
			// See potions above
			return;
		}

		final ConfigurationSection potions = config.getConfigurationSection("potions");

		if (potions == null) {
			return;
		}

		for (String effectName : potions.getKeys(false)) {
			final PotionEffectType type = PotionEffectType.getByName(effectName);
			if (type == null) {
				continue;
			}
			final PotionEffect potion = new PotionEffect(type,
					potions.getInt(effectName + ".duration"),
					potions.getInt(effectName + ".amplifier"),
					potions.getBoolean(effectName + ".ambient", true),
					potions.getBoolean(effectName + ".particles", true),
					potions.getBoolean(effectName + ".icon", false));
			player.addPotionEffect(potion, true);
		}
	}

	public void savePlayerInventory(final Player player) {
		final PlayerInventory inv = player.getInventory();
		final YamlConfiguration config = new YamlConfiguration();

		ItemStack[] contents = inv.getContents();
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

		contents = player.getEnderChest().getContents();
		for (int i = 0; i < contents.length; i++) {
			try {
				config.set("enderchest." + i, contents[i]);
			} catch (Exception e) {
				// If an exception is thrown, it's probably a Spigot serialization issue. Log it and move on.
				plugin.getLogger().severe(String.format("Unable to save data for %s's %s in enderchest slot %s",
						player.getName(), contents[i].getType(), i));
				e.printStackTrace();
			}
		}

		config.set("health", player.getHealth());
		config.set("food", player.getFoodLevel());
		config.set("saturation", (double) player.getSaturation());
		config.set("level", player.getLevel());
		config.set("levelProgress", (double) player.getExp());
		config.set("fireTicks", player.getFireTicks());
		config.set("fallDistance", player.getFallDistance());

		final ConfigurationSection potions = config.createSection("potions");
		for (PotionEffect effect : player.getActivePotionEffects()) {
			final String type = effect.getType().getName();
			potions.set(type + ".duration", effect.getDuration());
			potions.set(type + ".amplifier", effect.getAmplifier());
			potions.set(type + ".ambient", effect.isAmbient());
			potions.set(type + ".particles", effect.hasParticles());
			potions.set(type + ".icon", effect.hasIcon());
		}

		final File userFile = getPlayerFile(player.getUniqueId(), player.getGameMode());
		try {
			config.save(userFile);
		} catch (IOException e) {
			plugin.getLogger().severe("Unable to save user data to " + userFile.getPath());
			e.printStackTrace();
		}

		final YamlConfiguration genericData = new YamlConfiguration();
		genericData.set("lastGameMode", player.getGameMode().name());
		try {
			genericData.save(getGenericFile(player.getUniqueId()));
		} catch (IOException e) {
			plugin.getLogger().severe("Unable to save generic user data to " + userFile.getPath());
			e.printStackTrace();
		}
	}
}
