package yt.helloworld.carrotshop.listener;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import yt.helloworld.carrotshop.CarrotShop;
import yt.helloworld.carrotshop.ShopsData;
import yt.helloworld.carrotshop.shop.Shop;


public class PlayerClickListener {
	@Listener
	public void onPlayerRightClick(InteractBlockEvent.Secondary.MainHand event, @First Player player)
	{
		Optional<Location<World>> optLoc = event.getTargetBlock().getLocation();
		if (!optLoc.isPresent())
			return;

		Optional<Shop> shop = ShopsData.getShop(optLoc.get());
		if (shop.isPresent()) {
			if (optLoc.get().getBlockType() == BlockTypes.STANDING_SIGN || optLoc.get().getBlockType() == BlockTypes.WALL_SIGN) {
				event.setCancelled(true);
				shop.get().trigger(player);
				Sponge.getScheduler().createTaskBuilder().delayTicks(1).execute(
						task -> {
							shop.get().update();
							task.cancel();
						}).submit(CarrotShop.getInstance());
			}
		}
	}

	@Listener
	public void onPlayerLeftClick(InteractBlockEvent.Primary.MainHand event, @First Player player)
	{
		Optional<Location<World>> optLoc = event.getTargetBlock().getLocation();
		if (!optLoc.isPresent())
			return;

		Optional<ItemStack> optItem = player.getItemInHand(HandTypes.MAIN_HAND);
		if (optItem.isPresent() && optItem.get().getItem().equals(ItemTypes.REDSTONE)) {
			if (optLoc.get().getBlockType() == BlockTypes.CHEST) {
				event.setCancelled(true);
				ShopsData.storeItemLocation(player, optLoc.get());
			} else if (optLoc.get().getBlockType() == BlockTypes.STANDING_SIGN || optLoc.get().getBlockType() == BlockTypes.WALL_SIGN) {
				event.setCancelled(true);
				Shop.build(player, optLoc.get());
			}
		} else {
			Optional<Shop> shop = ShopsData.getShop(optLoc.get());
			if (shop.isPresent()) {
				shop.get().info(player);
				if (player.gameMode().get().equals(GameModes.CREATIVE) && (!optItem.isPresent() || !optItem.get().getItem().equals(ItemTypes.BEDROCK)))
					event.setCancelled(true);
			}
		}
	}
}
