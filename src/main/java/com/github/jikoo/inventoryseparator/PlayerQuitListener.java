package com.github.jikoo.inventoryseparator;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for PlayerQuitEvents.
 * 
 * @author Jikoo
 */
public class PlayerQuitListener implements Listener {

	private final InventorySeparator plugin;

	public PlayerQuitListener(final InventorySeparator plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent event) {
		final InventoryGroup group = this.plugin.getWorldGroup(event.getPlayer().getWorld().getName());
		group.savePlayerInventory(event.getPlayer());
	}
}
