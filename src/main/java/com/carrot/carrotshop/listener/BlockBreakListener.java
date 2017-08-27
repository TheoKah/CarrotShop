package com.carrot.carrotshop.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.carrot.carrotshop.ShopsData;
import com.carrot.carrotshop.shop.Shop;

public class BlockBreakListener {

	@Listener
	public void onBlockBreak(ChangeBlockEvent.Break event) {
		for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
			if (transaction.isValid()) {
				Optional<Location<World>> loc = transaction.getOriginal().getLocation();
				if (loc.isPresent()) {
					Optional<List<Shop>> shops = ShopsData.getShops(loc.get());
					if (shops.isPresent()) {
						Optional<Player> cause = event.getCause().first(Player.class);
						if (!cause.isPresent()) {
							event.setCancelled(true);
						}
						List<Shop> toDelete = new ArrayList<>();
						shops.get().forEach((shop) -> {
							toDelete.add(shop);
						});
						toDelete.forEach((shop) -> {
							if (!shop.destroy(cause.get()))
								event.setCancelled(true);
						});
					}
				}
			}
		}
	}

	@Listener(order = Order.FIRST, beforeModifications = true)
	public void onSignChanged(ChangeSignEvent event)
	{
		Optional<List<Shop>> shops = ShopsData.getShops(event.getTargetTile().getLocation());
		if (shops.isPresent()) {
			event.setCancelled(true);
		}
	}
}
