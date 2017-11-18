package com.carrot.carrotshop.shop;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Stack;

import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
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
import com.carrot.carrotshop.ShopsData;
import com.carrot.carrotshop.ShopsLogs;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class iSell extends Shop {
	@Setting
	private Inventory itemsTemplate;
	@Setting
	private int price;

	public iSell() {
	}

	public iSell(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		if (!player.hasPermission("carrotshop.admin.isell"))
			throw new ExceptionInInitializerError("You don't have perms to build an iTrade sign");
		Stack<Location<World>> locations = ShopsData.getItemLocations(player);
		if (locations.isEmpty())
			throw new ExceptionInInitializerError("iSell signs require a chest");
		Optional<TileEntity> chestOpt = locations.peek().getTileEntity();
		if (!chestOpt.isPresent() || !(chestOpt.get() instanceof TileEntityCarrier))
			throw new ExceptionInInitializerError("iSell signs require a chest");
		Inventory items = ((TileEntityCarrier) chestOpt.get()).getInventory();
		if (items.totalItems() == 0)
			throw new ExceptionInInitializerError("chest cannot be empty");
		price = getPrice(sign);
		if (price < 0)
			throw new ExceptionInInitializerError("bad price");
		itemsTemplate = Inventory.builder().from(items).build(CarrotShop.getInstance());
		for(Inventory item : items.slots()) {
			if (item.peek().isPresent())
				itemsTemplate.offer(item.peek().get());
		}

		ShopsData.clearItemLocations(player);
		player.sendMessage(Text.of(TextColors.DARK_GREEN, "You have setup an iSell shop:"));
		done(player);
		info(player);
	}

	@Override
	public void info(Player player) {
		Builder builder = Text.builder();
		builder.append(Text.of("Sell"));
		for (Inventory item : itemsTemplate.slots()) {
			if (item.peek().isPresent()) {
				builder.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
			}
		}
		builder.append(Text.of(" for ", formatPrice(price), "?"));
		player.sendMessage(builder.build());
		update();
	}

	@Override
	public boolean trigger(Player player) {
		Inventory inv = player.getInventory().query(InventoryRow.class);
		
		if (!hasEnough(inv, itemsTemplate)) {
			player.sendMessage(Text.of(TextColors.DARK_RED, "You don't have the items to sell!"));
			return false;
		}

		Builder itemsName = Text.builder();
		for (Inventory item : itemsTemplate.slots()) {
			if (item.peek().isPresent()) {
				Optional<ItemStack> template = getTemplate(inv, item.peek().get());
				if (template.isPresent()) {
					itemsName.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
					Optional<ItemStack> items = inv.queryAny(template.get()).poll(item.peek().get().getQuantity());
					if (!items.isPresent()) {
						return false;
					}
				}
			}
		}

		UniqueAccount sellerAccount = CarrotShop.getEcoService().getOrCreateAccount(player.getUniqueId()).get();
		TransactionResult result = sellerAccount.deposit(getCurrency(), BigDecimal.valueOf(price), CarrotShop.getCause());
		if (result.getResult() != ResultType.SUCCESS) {
			player.sendMessage(Text.of(TextColors.DARK_RED, "Unable to give you the money!"));
			return false;
		}
		
		ShopsLogs.log(getOwner(), player, "sell", super.getLocation(), Optional.of(price), getRawCurrency(), Optional.of(itemsTemplate), Optional.empty());

		player.sendMessage(Text.of("You sold", itemsName.build(), " for ", formatPrice(price)));
		
		return true;
	}

}
