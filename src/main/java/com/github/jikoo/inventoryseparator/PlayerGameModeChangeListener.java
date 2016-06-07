package com.github.jikoo.inventoryseparator;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

/**
 * I feel as though the name of this class is sufficiently descriptive.
 * 
 * @author Jikoo
 */
public class PlayerGameModeChangeListener implements Listener {

	private final InventorySeparator plugin;

	protected PlayerGameModeChangeListener(final InventorySeparator plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlayerGameModeChange(final PlayerGameModeChangeEvent event) {
		final Player player = event.getPlayer();
		if (player.hasPermission("inventoryseparator.ignoreinventoryswap")
				|| player.hasPermission("inventoryseparator.ignoregamemode")) {
			return;
		}
		final InventoryGroup group = this.plugin.getWorldGroup(player.getWorld().getName());
		group.savePlayerInventory(player);
		group.changePlayerInventory(player, event.getNewGameMode());
	}
}
