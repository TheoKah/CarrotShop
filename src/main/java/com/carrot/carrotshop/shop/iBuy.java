package com.carrot.carrotshop.shop;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Stack;

import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.type.InventoryRow;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.carrot.carrotshop.CarrotShop;
import com.carrot.carrotshop.Lang;
import com.carrot.carrotshop.ShopsData;
import com.carrot.carrotshop.ShopsLogs;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class iBuy extends Shop {
	@Setting
	private Inventory itemsTemplate;
	@Setting
	private float price;

	static private String type = "iBuy";
	
	public iBuy() {
	}

	public iBuy(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		if (!player.hasPermission("carrotshop.admin.ibuy"))
			throw new ExceptionInInitializerError(Lang.SHOP_PERM.replace("%type%", type));
		Stack<Location<World>> locations = ShopsData.getItemLocations(player);
		if (locations.isEmpty())
			throw new ExceptionInInitializerError(Lang.SHOP_CHEST.replace("%type%", type));
		Optional<TileEntity> chestOpt = locations.peek().getTileEntity();
		if (!chestOpt.isPresent() || !(chestOpt.get() instanceof TileEntityCarrier))
			throw new ExceptionInInitializerError(Lang.SHOP_CHEST.replace("%type%", type));
		Inventory items = ((TileEntityCarrier) chestOpt.get()).getInventory();
		if (items.totalItems() == 0)
			throw new ExceptionInInitializerError(Lang.SHOP_CHEST_EMPTY);
		price = getPrice(sign);
		if (price < 0)
			throw new ExceptionInInitializerError(Lang.SHOP_PRICE);
		itemsTemplate = Inventory.builder().from(items).build(CarrotShop.getInstance());
		for(Inventory item : items.slots()) {
			if (item.peek().isPresent())
				itemsTemplate.offer(item.peek().get());
		}

		ShopsData.clearItemLocations(player);
		player.sendMessage(Text.of(TextColors.DARK_GREEN, Lang.SHOP_DONE.replace("%type%", type)));
		done(player);
		info(player);
	}
	
	public iBuy(Location<World> sign, ItemStackSnapshot item) {
		super(sign);
		price = getPrice(sign);
		if (price < 0)
			price = 0;
		itemsTemplate = Inventory.builder().build(CarrotShop.getInstance());
		itemsTemplate.offer(item.createStack());
	}

	@Override
	public void info(Player player) {
		Builder builder = Text.builder();
		builder.append(Text.of(Lang.split(Lang.SHOP_FORMAT_BUY, "%items%", 0).replace("%price%", formatPrice(price))));
		for (Inventory item : itemsTemplate.slots()) {
			if (item.peek().isPresent()) {
				builder.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
			}
		}
		builder.append(Text.of(Lang.split(Lang.SHOP_FORMAT_BUY, "%items%", 1).replace("%price%", formatPrice(price))));
		player.sendMessage(builder.build());
		update();
	}
	@Override
	public boolean trigger(Player player) {
		UniqueAccount buyerAccount = CarrotShop.getEcoService().getOrCreateAccount(player.getUniqueId()).get();
		TransactionResult result = buyerAccount.withdraw(getCurrency(), BigDecimal.valueOf(price), CarrotShop.getCause());
		if (result.getResult() != ResultType.SUCCESS) {
			player.sendMessage(Text.of(TextColors.DARK_RED, Lang.SHOP_MONEY));
			return false;
		}
		Inventory inv = player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(InventoryRow.class));

		Builder itemsName = Text.builder();
		for (Inventory item : itemsTemplate.slots()) {
			if (item.peek().isPresent()) {
				itemsName.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
				inv.offer(item.peek().get().copy()).getRejectedItems().forEach(action -> {
					putItemInWorld(action, player.getLocation());
				});
			}
		}

		ShopsLogs.log(getOwner(), player, "buy", super.getLocation(), Optional.of(price), getRawCurrency(), Optional.of(itemsTemplate), Optional.empty());
		
		String recap = Lang.SHOP_RECAP_BUY.replace("%price%", formatPrice(price));
		player.sendMessage(Text.of(Lang.split(recap, "%items%", 0), itemsName.build(), Lang.split(recap, "%items%", 1)));

		return true;
	}

}
