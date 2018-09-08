package com.carrot.carrotshop.shop;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
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
import com.carrot.carrotshop.ShopConfig;
import com.carrot.carrotshop.Lang;
import com.carrot.carrotshop.ShopsData;
import com.carrot.carrotshop.ShopsLogs;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class Buy extends Shop {
	@Setting
	private Inventory itemsTemplate;
	@Setting
	private Location<World> sellerChest;
	@Setting
	private int price;

	static private String type = "Buy";

	public Buy() {
	}

	public Buy(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		if (!player.hasPermission("carrotshop.create.buy"))
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
		int cost = ShopConfig.getNode("cost", type).getInt(0);
		if (cost > 0) {
			UniqueAccount buyerAccount = CarrotShop.getEcoService().getOrCreateAccount(player.getUniqueId()).get();
			TransactionResult result = buyerAccount.withdraw(getCurrency(), BigDecimal.valueOf(cost), CarrotShop.getCause());
			if (result.getResult() != ResultType.SUCCESS)
				throw new ExceptionInInitializerError(Lang.SHOP_COST.replace("%type%", type).replace("%cost%", getCurrency().format(BigDecimal.valueOf(cost), 0).toPlain()));
		}
		sellerChest = locations.peek();
		itemsTemplate = Inventory.builder().from(items).build(CarrotShop.getInstance());
		for(Inventory item : items.slots()) {
			if (item.peek().isPresent())
				itemsTemplate.offer(item.peek().get());
		}
		setOwner(player);
		ShopsData.clearItemLocations(player);
		if (cost > 0)
			player.sendMessage(Text.of(TextColors.DARK_GREEN, Lang.SHOP_DONE_COST.replace("%type%", type).replace("%cost%", getCurrency().format(BigDecimal.valueOf(cost), 0).toPlain())));
		else
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
			if (hasEnough(((TileEntityCarrier) chest.get()).getInventory(), itemsTemplate)) {
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
		builder.append(Text.of(Lang.split(Lang.SHOP_FORMAT_BUY, "%items%", 0).replace("%price%", formatPrice(price))));
		for (Inventory item : itemsTemplate.slots()) {
			if (item.peek().isPresent()) {
				builder.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
			}
		}
		builder.append(Text.of(Lang.split(Lang.SHOP_FORMAT_BUY, "%items%", 1).replace("%price%", formatPrice(price))));
		player.sendMessage(builder.build());
		if (!update())
			player.sendMessage(Text.of(TextColors.GOLD, Lang.SHOP_EMPTY));

	}
	@Override
	public boolean trigger(Player player) {
		Optional<TileEntity> chestToGive = sellerChest.getTileEntity();
		if (chestToGive.isPresent() && chestToGive.get() instanceof TileEntityCarrier) {
			if (!hasEnough(((TileEntityCarrier) chestToGive.get()).getInventory(), itemsTemplate)) {
				player.sendMessage(Text.of(TextColors.GOLD, Lang.SHOP_EMPTY));
				update();
				return false;
			}
		} else {
			return false;
		}
		UniqueAccount buyerAccount = CarrotShop.getEcoService().getOrCreateAccount(player.getUniqueId()).get();
		UniqueAccount sellerAccount = CarrotShop.getEcoService().getOrCreateAccount(getOwner()).get();
		float tax = ShopConfig.getNode("taxes", type).getFloat(0);
		TransactionResult accountResult;
		if (tax > 0) {
			accountResult = buyerAccount.withdraw(getCurrency(), BigDecimal.valueOf(price), CarrotShop.getCause());
			if (accountResult.getResult() == ResultType.SUCCESS) {
				accountResult = sellerAccount.deposit(getCurrency(), BigDecimal.valueOf(price - price * tax / 100), CarrotShop.getCause());
			}

		} else {
			accountResult = buyerAccount.transfer(sellerAccount, getCurrency(), BigDecimal.valueOf(price), CarrotShop.getCause());
		}
		if (accountResult.getResult() != ResultType.SUCCESS) {
			player.sendMessage(Text.of(TextColors.DARK_RED, Lang.SHOP_MONEY));
			return false;
		}
		Inventory inv = player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(InventoryRow.class));

		Inventory invToGive = ((TileEntityCarrier) chestToGive.get()).getInventory();

		Builder itemsName = Text.builder();

		for (Inventory item : itemsTemplate.slots()) {
			if (item.peek().isPresent()) {
				Optional<ItemStack> template = getTemplate(invToGive, item.peek().get());
				if (template.isPresent()) {
					itemsName.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
					Optional<ItemStack> items = invToGive.query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(template.get())).poll(item.peek().get().getQuantity());
					if (items.isPresent()) {
						inv.offer(items.get()).getRejectedItems().forEach(action -> {
							putItemInWorld(action, player.getLocation());
						});
					} else {
						return false;
					}
				}
			}
		}

		ShopsLogs.log(getOwner(), player, "buy", super.getLocation(), Optional.of(price), getRawCurrency(), Optional.of(itemsTemplate), Optional.empty());

		String recap = Lang.SHOP_RECAP_BUY.replace("%price%", formatPrice(price));
		player.sendMessage(Text.of(Lang.split(recap, "%items%", 0), itemsName.build(), Lang.split(recap, "%items%", 1)));

		if (!CarrotShop.noSpam(getOwner())) {
			Optional<Player> seller = Sponge.getServer().getPlayer(getOwner());
			if (seller.isPresent()) {
				recap = Lang.SHOP_RECAP_OBUY.replace("%player%", player.getName()).replace("%price%", formatPrice(price));
				seller.get().sendMessage(Text.of(Lang.split(recap, "%items%", 0), itemsName.build(), Lang.split(recap, "%items%", 1)));

			}
		}

		update();
		return true;
	}

}
