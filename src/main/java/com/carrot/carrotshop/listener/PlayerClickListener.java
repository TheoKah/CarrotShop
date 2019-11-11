package com.carrot.carrotshop.listener;

import java.util.List;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.carrot.carrotshop.CarrotShop;
import com.carrot.carrotshop.Lang;
import com.carrot.carrotshop.ShopConfig;
import com.carrot.carrotshop.ShopsData;
import com.carrot.carrotshop.shop.Shop;


public class PlayerClickListener {
	@Listener(order=Order.AFTER_PRE, beforeModifications = true)
	public void onPlayerRightClick(InteractBlockEvent.Secondary.MainHand event, @First Player player)
	{
		Optional<Location<World>> optLoc = event.getTargetBlock().getLocation();
		if (!optLoc.isPresent())
			return;

		Optional<List<Shop>> shops = ShopsData.getShops(optLoc.get());
		if (shops.isPresent()) {
			
			if (ShopConfig.getNode("others", "emptyhand").getBoolean() && (player.getItemInHand(HandTypes.MAIN_HAND).isPresent() || player.getItemInHand(HandTypes.OFF_HAND).isPresent())){
				player.sendMessage(Text.of(TextColors.DARK_RED, Lang.SHOP_EMPTYHAND));
				return;
			}
			
			shops.get().forEach((shop) -> {
				if (shop.getLocation().equals(optLoc.get())) {
					shop.trigger(player);
					if (!shop.isOwner(player))
						event.setCancelled(true);
					Sponge.getScheduler().createTaskBuilder().delayTicks(4).execute(
							task -> {
								shop.update();
								task.cancel();
							}).submit(CarrotShop.getInstance());
				}
			});
		}
	}

	@Listener(order=Order.AFTER_PRE, beforeModifications = true)
	public void onPlayerLeftClickMaster(InteractBlockEvent.Primary.MainHand event, @First Player player)
	{
		Optional<Location<World>> optLoc = event.getTargetBlock().getLocation();
		if (!optLoc.isPresent())
			return;

		Optional<List<Shop>> shops = ShopsData.getShops(optLoc.get());
		if (shops.isPresent()) {
			shops.get().forEach((shop) -> {
				if (shop.getLocation().equals(optLoc.get())) {
					shop.info(player);
					if (!shop.isOwner(player))
						event.setCancelled(true);
				}
			});
		}
	}

	@Listener(order=Order.FIRST, beforeModifications = true)
	public void onPlayerLeftClickProtect(InteractBlockEvent.Primary.MainHand event, @First Player player)
	{
		if (!player.gameMode().get().equals(GameModes.CREATIVE))
			return;

		Optional<Location<World>> optLoc = event.getTargetBlock().getLocation();
		if (!optLoc.isPresent())
			return;

		Optional<List<Shop>> shop = ShopsData.getShops(optLoc.get());
		if (shop.isPresent()) {
			Optional<ItemStack> optItem = player.getItemInHand(HandTypes.MAIN_HAND);

			if (!optItem.isPresent() || (!optItem.get().getItem().equals(ItemTypes.BEDROCK) && !optItem.get().getItem().equals(ItemTypes.REDSTONE) && !optItem.get().getItem().equals(ItemTypes.STICK))) {
				event.setCancelled(true);
			}
		}
	}

	@Listener(beforeModifications = true)
	public void onPlayerLeftClickNormal(InteractBlockEvent.Primary.MainHand event, @First Player player)
	{
		Optional<Location<World>> optLoc = event.getTargetBlock().getLocation();
		if (!optLoc.isPresent())
			return;

		Optional<ItemStack> optItem = player.getItemInHand(HandTypes.MAIN_HAND);
		if (optItem.isPresent() && optItem.get().getItem().equals(ItemTypes.REDSTONE)) {
			if (optLoc.get().getBlockType() == BlockTypes.CHEST || optLoc.get().getBlockType() == BlockTypes.TRAPPED_CHEST || optLoc.get().getBlockType() == BlockTypes.LEVER) {
				event.setCancelled(true);
				ShopsData.storeItemLocation(player, optLoc.get());
			} else if (optLoc.get().getBlockType() == BlockTypes.STANDING_SIGN || optLoc.get().getBlockType() == BlockTypes.WALL_SIGN) {
				event.setCancelled(true);
				Shop.build(player, optLoc.get());
			}
		} else if (ShopsData.hasMultipleCurrencies() && optItem.isPresent() && optItem.get().getItem().equals(ItemTypes.STICK)
				&& (optLoc.get().getBlockType() == BlockTypes.STANDING_SIGN || optLoc.get().getBlockType() == BlockTypes.WALL_SIGN)) {
			Optional<List<Shop>> optShop = ShopsData.getShops(optLoc.get());
			if (optShop.isPresent()) {
				event.setCancelled(true);
				for (Shop shop : optShop.get()) {
					if (shop.canLoopCurrency(player)) {
						shop.loopCurrency();
						if (shop.hasCurrency())
							player.sendMessage(Text.of(TextColors.DARK_GREEN, Lang.split(Lang.CURRENCY_VALUE, "%url%", 0), TextColors.YELLOW, shop.getCurrency().getDisplayName(), TextColors.DARK_GREEN, Lang.split(Lang.CURRENCY_VALUE, "%url%", 1)));
						else
							player.sendMessage(Text.of(TextColors.DARK_GREEN, Lang.CURRENCY_SERVER));							
					}
				}
			}
		}
	}
}
