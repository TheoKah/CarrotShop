package com.carrot.carrotshop.listener;

import java.util.Optional;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
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
					Optional<Shop> shop = ShopsData.getShop(loc.get());
					if (shop.isPresent()) {
						Optional<Player> cause = event.getCause().first(Player.class);
						if (cause.isPresent()) {
							if (!shop.get().destroy(cause.get()))
								event.setCancelled(true);
						} else {
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}
}
