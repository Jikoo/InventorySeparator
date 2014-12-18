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

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		if (event.getPlayer().hasPermission("inventoryseparator.ignoreinventoryswap")) {
			return;
		}
		InventoryGroup from = InventorySeparator.getInstance().getWorldGroup(event.getFrom().getName());
		InventoryGroup to = InventorySeparator.getInstance().getWorldGroup(event.getPlayer().getWorld().getName());
		if (from.equals(to)) {
			if (!event.getPlayer().hasPermission("inventoryseparator.ignoregamemode")
					&& event.getPlayer().getGameMode() != to.getDefaultGameMode()) {
				// PlayerGameModeChangedListener will handle the swap, same group
				event.getPlayer().setGameMode(to.getDefaultGameMode());
			}
			return;
		}
		from.savePlayerInventory(event.getPlayer());
		// Swapping world groups, change to the inventory for the world being entered in the current GameMode
		to.changePlayerInventory(event.getPlayer(), event.getPlayer().getGameMode());
		if (!event.getPlayer().hasPermission("inventoryseparator.ignoregamemode")
				&& event.getPlayer().getGameMode() != to.getDefaultGameMode()) {
			// PlayerGameModeChangedListener will swap inventory to the correct GameMode inventory
			event.getPlayer().setGameMode(to.getDefaultGameMode());
		}
	}
}
