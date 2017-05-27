package com.carrot.carrotshop.shop;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
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

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class Sell extends Shop {
	@Setting
	private Inventory itemsTemplate;
	@Setting
	private Location<World> sellerChest;
	@Setting
	private int price;

	public Sell() {
	}

	public Sell(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		Stack<Location<World>> locations = ShopsData.getItemLocations(player);
		if (locations.isEmpty())
			throw new ExceptionInInitializerError("Sell signs require a chest");
		Optional<TileEntity> chestOpt = locations.peek().getTileEntity();
		if (!chestOpt.isPresent() || !(chestOpt.get() instanceof TileEntityCarrier))
			throw new ExceptionInInitializerError("Sell signs require a chest");
		Inventory items = ((TileEntityCarrier) chestOpt.get()).getInventory();
		if (items.totalItems() == 0)
			throw new ExceptionInInitializerError("chest cannot be empty");
		price = getPrice(sign);
		if (price < 0)
			throw new ExceptionInInitializerError("bad price");
		sellerChest = locations.peek();
		itemsTemplate = Inventory.builder().from(items).build(CarrotShop.getInstance());
		for(Inventory item : items.slots()) {
			if (item.peek().isPresent())
				itemsTemplate.offer(item.peek().get());
		}
		setOwner(player);
		ShopsData.clearItemLocations(player);
		player.sendMessage(Text.of(TextColors.DARK_GREEN, "You have setup a Sell shop:"));
		info(player);
	}

	@Override
	public List<Location<World>> getLocations() {
		List<Location<World>> locations = super.getLocations();
		locations.add(sellerChest);
		return locations;
	}
	
	@Override
	public boolean update() {
		Optional<TileEntity> chest = sellerChest.getTileEntity();
		if (chest.isPresent() && chest.get() instanceof TileEntityCarrier) {
			Inventory chestInv = ((TileEntityCarrier) chest.get()).getInventory();
			if (chestInv.capacity() - chestInv.size() >= itemsTemplate.size()) {
				setOK();
				return true;
			}
		}
		setFail();
		return false;
	}

	@Override
	public void info(Player player) {
		Builder builder = Text.builder();
		builder.append(Text.of("Sell"));
		for (Inventory item : itemsTemplate.slots()) {
			if (item.peek().isPresent()) {
				builder.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getItem().getTranslation().get(), " x", item.peek().get().getQuantity()));
			}
		}
		builder.append(Text.of(" for ", price, " ", CarrotShop.getEcoService().getDefaultCurrency().getPluralDisplayName(), "?"));
		player.sendMessage(builder.build());
		if (!update())
			player.sendMessage(Text.of(TextColors.GOLD, "This shop is full!"));

	}
	@Override
	public boolean trigger(Player player) {
		if (!hasEnough(player.getInventory(), itemsTemplate)) {
			player.sendMessage(Text.of(TextColors.DARK_RED, "You don't have the items to sell!"));
			return false;
		}
		Optional<TileEntity> chest = sellerChest.getTileEntity();
		if (chest.isPresent() && chest.get() instanceof TileEntityCarrier) {
			Inventory chestInv = ((TileEntityCarrier) chest.get()).getInventory();
			if (chestInv.capacity() - chestInv.size() < itemsTemplate.size()) {
				player.sendMessage(Text.of(TextColors.GOLD, "This shop is full!"));
				update();
				return false;
			}
		}

		Inventory inv = player.getInventory().query(InventoryRow.class);
		Inventory invChest = ((TileEntityCarrier) chest.get()).getInventory();

		for (Inventory item : itemsTemplate.slots()) {
			if (item.peek().isPresent()) {
				Optional<ItemStack> items = inv.query(item.peek().get()).poll(item.peek().get().getQuantity());
				if (items.isPresent()) {
					invChest.offer(items.get());
				} else {
					return false;
				}
			}
		}
		
		UniqueAccount sellerAccount = CarrotShop.getEcoService().getOrCreateAccount(player.getUniqueId()).get();
		UniqueAccount buyerAccount = CarrotShop.getEcoService().getOrCreateAccount(getOwner()).get();
		TransactionResult result = buyerAccount.transfer(sellerAccount, CarrotShop.getEcoService().getDefaultCurrency(), BigDecimal.valueOf(price), Cause.source(this).build());
		if (result.getResult() != ResultType.SUCCESS) {
			player.sendMessage(Text.of(TextColors.DARK_RED, "Seller don't have enough money!"));
			return false;
		}
		
		update();
		return true;
	}

}
