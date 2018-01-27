package com.carrot.carrotshop.shop;

import java.math.BigDecimal;
import java.util.List;
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
import com.carrot.carrotshop.Lang;
import com.carrot.carrotshop.ShopsData;
import com.carrot.carrotshop.ShopsLogs;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class aSell extends Shop {
	@Setting
	private Inventory itemsTemplate;
	@Setting
	private Location<World> sellerChest;
	@Setting
	private int price;
	
	static private String type = "aSell";

	public aSell() {
	}

	public aSell(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		if (!player.hasPermission("carrotshop.admin.asell"))
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
		sellerChest = locations.peek();
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
		builder.append(Text.of(Lang.split(Lang.SHOP_FORMAT_SELL, "%items%", 0).replace("%price%", formatPrice(price))));
		for (Inventory item : itemsTemplate.slots()) {
			if (item.peek().isPresent()) {
				builder.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
			}
		}
		builder.append(Text.of(Lang.split(Lang.SHOP_FORMAT_SELL, "%items%", 1).replace("%price%", formatPrice(price))));
		player.sendMessage(builder.build());
		if (!update())
			player.sendMessage(Text.of(TextColors.GOLD, Lang.SHOP_FULL));

	}
	@Override
	public boolean trigger(Player player) {
		Inventory inv = player.getInventory().query(InventoryRow.class);
		
		if (!hasEnough(inv, itemsTemplate)) {
			player.sendMessage(Text.of(TextColors.DARK_RED, Lang.SHOP_ITEMS));
			return false;
		}
		Optional<TileEntity> chest = sellerChest.getTileEntity();
		if (chest.isPresent() && chest.get() instanceof TileEntityCarrier) {
			Inventory chestInv = ((TileEntityCarrier) chest.get()).getInventory();
			if (chestInv.capacity() - chestInv.size() < itemsTemplate.size()) {
				player.sendMessage(Text.of(TextColors.GOLD, Lang.SHOP_FULL));
				update();
				return false;
			}
		}
		
		Inventory invChest = ((TileEntityCarrier) chest.get()).getInventory();

		Builder itemsName = Text.builder();
		for (Inventory item : itemsTemplate.slots()) {
			if (item.peek().isPresent()) {
				Optional<ItemStack> template = getTemplate(inv, item.peek().get());
				if (template.isPresent()) {
					itemsName.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
					Optional<ItemStack> items = inv.queryAny(template.get()).poll(item.peek().get().getQuantity());
					if (items.isPresent()) {
						invChest.offer(items.get());
					} else {
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

		update();
		return true;
	}

}
