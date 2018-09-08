package com.carrot.carrotshop.shop;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Stack;

import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
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
public class iSell extends Shop {
	@Setting
	private Inventory itemsTemplate;
	@Setting
	private float price;

	static private String type = "iSell";
	
	public iSell() {
	}

	public iSell(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		if (!player.hasPermission("carrotshop.admin.isell"))
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
	
	public iSell(Location<World> sign, ItemStackSnapshot item) {
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
		builder.append(Text.of(Lang.split(Lang.SHOP_FORMAT_SELL, "%items%", 0).replace("%price%", formatPrice(price))));
		for (Inventory item : itemsTemplate.slots()) {
			if (item.peek().isPresent()) {
				builder.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
			}
		}
		builder.append(Text.of(Lang.split(Lang.SHOP_FORMAT_SELL, "%items%", 1).replace("%price%", formatPrice(price))));
		player.sendMessage(builder.build());
		update();
	}

	@Override
	public boolean trigger(Player player) {
		Inventory inv = player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(InventoryRow.class));
		
		if (!hasEnough(inv, itemsTemplate)) {
			player.sendMessage(Text.of(TextColors.DARK_RED, Lang.SHOP_ITEMS));
			return false;
		}

		Builder itemsName = Text.builder();
		for (Inventory item : itemsTemplate.slots()) {
			if (item.peek().isPresent()) {
				Optional<ItemStack> template = getTemplate(inv, item.peek().get());
				if (template.isPresent()) {
					itemsName.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
					Optional<ItemStack> items = inv.query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(template.get())).poll(item.peek().get().getQuantity());
					if (!items.isPresent()) {
						return false;
					}
				}
			}
		}

		UniqueAccount sellerAccount = CarrotShop.getEcoService().getOrCreateAccount(player.getUniqueId()).get();
		TransactionResult result = sellerAccount.deposit(getCurrency(), BigDecimal.valueOf(price), CarrotShop.getCause());
		if (result.getResult() != ResultType.SUCCESS) {
			player.sendMessage(Text.of(TextColors.DARK_RED, Lang.SHOP_ERROR_MONEY));
			return false;
		}
		
		ShopsLogs.log(getOwner(), player, "sell", super.getLocation(), Optional.of(price), getRawCurrency(), Optional.of(itemsTemplate), Optional.empty());

		String recap = Lang.SHOP_RECAP_SELL.replace("%price%", formatPrice(price));
		player.sendMessage(Text.of(Lang.split(recap, "%items%", 0), itemsName.build(), Lang.split(recap, "%items%", 1)));

		return true;
	}

}
