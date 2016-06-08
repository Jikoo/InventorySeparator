package com.github.jikoo.inventoryseparator;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

/**
 * Listener for PlayerChangedWorldEvents.
 * 
 * @author Jikoo
 */
public class PlayerChangedWorldListener implements Listener {

	private final InventorySeparator plugin;

	protected PlayerChangedWorldListener(final InventorySeparator plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
		final InventoryGroup from = this.plugin.getWorldGroup(event.getFrom().getName());
		final InventoryGroup to = this.plugin.getWorldGroup(event.getPlayer().getWorld().getName());
		if (from.equals(to)) {
			if (event.getPlayer().getGameMode() == to.getDefaultGameMode()) {
				return;
			}
			// PlayerGameModeChangedListener will handle the swap, same group
			if (!event.getPlayer().hasPermission("inventoryseparator.ignoregamemode")) {
				event.getPlayer().setGameMode(to.getDefaultGameMode());
			} else if (event.getPlayer().hasPermission("inventoryseparator.uselastgamemode")) {
				event.getPlayer().setGameMode(to.getLastGameMode(event.getPlayer().getUniqueId()));
			}
			return;
		}
		if (!event.getPlayer().hasPermission("inventoryseparator.ignoreinventoryswap")) {
			from.savePlayerInventory(event.getPlayer());
			// Swapping world groups, change to the inventory for the world being entered in the current GameMode
			to.changePlayerInventory(event.getPlayer(), event.getPlayer().getGameMode());
		}
		if (event.getPlayer().getGameMode() != to.getDefaultGameMode()) {
			// PlayerGameModeChangedListener will swap inventory to the correct GameMode inventory
			if (!event.getPlayer().hasPermission("inventoryseparator.ignoregamemode")) {
				event.getPlayer().setGameMode(to.getDefaultGameMode());
			} else if (event.getPlayer().hasPermission("inventoryseparator.uselastgamemode")) {
				event.getPlayer().setGameMode(to.getLastGameMode(event.getPlayer().getUniqueId()));
			}
		}
	}
}
