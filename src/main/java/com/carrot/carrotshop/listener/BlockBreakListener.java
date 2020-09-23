package com.carrot.carrotshop.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.carrot.carrotshop.ShopsData;
import com.carrot.carrotshop.shop.Shop;

public class BlockBreakListener {
	boolean preCanceled = false;
	int preCanceledTick = -1;

	private void addSignAndShop(List<Shop> allShops, Location<World> loc, Direction dir, boolean wall) {
		if (loc.getRelative(dir).getBlockType() == (wall ? BlockTypes.WALL_SIGN : BlockTypes.STANDING_SIGN)) {
			if (wall) {
				Location<World> sign = loc.getRelative(dir);
				if (sign.supports(Keys.DIRECTION)) {
					Optional<Direction> direction = sign.get(Keys.DIRECTION);
					if (direction.isPresent()) {
						if (direction.get() != dir)
							return;
					}
				}
			}
			Optional<List<Shop>> shops = ShopsData.getShops(loc.getRelative(dir));
			if (shops.isPresent())
				allShops.addAll(shops.get());
		}
	}

	@Listener(order = Order.FIRST, beforeModifications = true)
	public void onBlockBreak(ChangeBlockEvent.Pre event) {
		List<Shop> allShops = new ArrayList<>();
		preCanceledTick = Sponge.getServer().getRunningTimeTicks();
		preCanceled = false;
		for (Location<World> loc : event.getLocations()) {
			Optional<List<Shop>> shops = ShopsData.getShops(loc);
			if (shops.isPresent()) {
				allShops.addAll(shops.get());
			}
			addSignAndShop(allShops, loc, Direction.UP, false);
			addSignAndShop(allShops, loc, Direction.NORTH, true);
			addSignAndShop(allShops, loc, Direction.SOUTH, true);
			addSignAndShop(allShops, loc, Direction.EAST, true);
			addSignAndShop(allShops, loc, Direction.WEST, true);
		}
		if (allShops.isEmpty())
			return;
		Optional<Player> cause = event.getCause().first(Player.class);
		if (cause.isPresent()) {
			allShops.forEach((shop) -> {
				if (!shop.destroy(cause.get())) {
					event.setCancelled(true);
					preCanceled = true;
				}
			});
		} else {
			event.setCancelled(true);
			preCanceled = true;
		}
	}

	@Listener(order = Order.FIRST, beforeModifications = true)
	public void onBlockBreak(ChangeBlockEvent.Break event) {
        if (preCanceled && preCanceledTick == Sponge.getServer().getRunningTimeTicks())
            event.setCancelled(true);
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
