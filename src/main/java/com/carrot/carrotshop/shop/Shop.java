package com.carrot.carrotshop.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.spawn.EntitySpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;

import com.carrot.carrotshop.CarrotShop;
import com.carrot.carrotshop.ShopsData;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public abstract class Shop {

	@Setting
	private UUID owner = null;
	@Setting
	private Location<World> location = null;
	@Setting
	private Currency currency = null;


	public Shop() {
	}

	public Shop(Location<World> loc) {
		Optional<TileEntity> tile = loc.getTileEntity();
		if (!tile.isPresent() || !tile.get().supports(SignData.class))
			throw new ExceptionInInitializerError("Improbable error: managed to trigger a shop creation event from something other than a sign");
		location = loc;
	}

	public abstract void info(Player player);
	public abstract boolean trigger(Player player);

	public final void done(Player player) {
		if (canLoopCurrency(player)) {
			player.sendMessage(Text.of(TextColors.GOLD, "This sign will use default currency: ", TextColors.YELLOW, getCurrency().getDisplayName()));
			player.sendMessage(Text.of(TextColors.GOLD, "Left click the sign with a stick to use another currency"));
		}
	}

	public boolean update() {
		setOK();
		return true;
	}

	public final boolean destroy(Player player) {
		if (isOwner(player)) {
			setReset();
			ShopsData.delShop(this);
			return true;
		}
		return false;
	}

	public Location<World> getLocation() {
		return location;
	}

	public List<Location<World>> getLocations() {
		List<Location<World>> locations = new ArrayList<>();
		locations.add(location);
		return locations;
	}

	public boolean canLoopCurrency(Player player) {
		return ShopsData.hasMultipleCurrencies() && isOwner(player) && player.hasPermission("carrotshop.setup.currency");
	}

	protected final void setOwner(Player player) {
		owner = player.getUniqueId();
	}

	protected final UUID getOwner() {
		return owner;
	}

	protected final boolean isOwner(Player player) {
		if (owner != null) {
			if (owner.equals(player.getUniqueId()))
				return true;
		}
		return player.hasPermission("carrotshop.admin.override");
	}

	protected final void setOK() {
		setFirstLineColor(TextColors.DARK_BLUE);
	}

	protected final void setFail() {
		setFirstLineColor(TextColors.RED);
	}

	public final void setReset() {
		setFirstLineColor(TextColors.RESET);
	}

	public final Currency getCurrency() {
		if (hasCurrency())
			return currency;
		return ShopsData.getCurrency();
	}

	protected final Optional<Currency> getRawCurrency() {
		if (hasCurrency())
			return Optional.of(currency);
		return Optional.empty();
	}

	public boolean hasCurrency() {
		return ShopsData.hasMultipleCurrencies() && currency != null;
	}

	public final void loopCurrency() {
		if (!ShopsData.hasMultipleCurrencies())
			return ;

		if (ShopsData.getCurrency().equals(currency)) {
			currency = null;
			return ;
		}

		boolean takeNext = false;
		Currency first = null;

		if (currency == null)
			currency = ShopsData.getCurrency();

		for (Currency cur : CarrotShop.getEcoService().getCurrencies()) {
			if (first == null)
				first = cur;
			if (takeNext) {
				currency = cur;
				return ;
			}
			if (cur.equals(currency))
				takeNext = true;
		}
		currency = first;
	}

	protected final String formatPrice(int price) {
		switch (price) {
		case 0:
			return "free";
		case 1:
			return price + " " + getCurrency().getDisplayName().toPlain();
		default:
			return price + " " + getCurrency().getPluralDisplayName().toPlain();
		}

	}

	private final void setFirstLineColor(TextColor color) {
		Optional<TileEntity> sign = location.getTileEntity();
		if (sign.isPresent() && sign.get().supports(SignData.class)) {
			Optional<SignData> data = sign.get().getOrCreate(SignData.class);
			if (data.isPresent()) {
				SignData signData = data.get();
				signData.set(signData.lines().set(0, Text.of(color, signData.lines().get(0).toPlain())));
				sign.get().offer(signData);
			}
		}
	}

	static public void putItemInWorld(ItemStackSnapshot itemStackSnapshop, Location<World> spawnLocation) {
		Extent extent = spawnLocation.getExtent();
		Entity item = extent.createEntity(EntityTypes.ITEM, spawnLocation.getPosition());
		item.offer(Keys.REPRESENTED_ITEM, itemStackSnapshop);
		extent.spawnEntity(item, Cause.source(EntitySpawnCause.builder()
				.entity(item).type(SpawnTypes.PLUGIN).build()).build());

	}

	static protected final int getPrice(Location<World> location) {
		Optional<TileEntity> sign = location.getTileEntity();
		if (sign.isPresent() && sign.get().supports(SignData.class)) {
			Optional<SignData> data = sign.get().get(SignData.class);
			if (data.isPresent()) {
				String priceLine = data.get().lines().get(3).toPlain().replaceAll("[^\\d]", "");
				if (priceLine.length() == 0)
					return 0;
				return Integer.parseInt(priceLine);
			}
		}
		return -1;
	}

	static public boolean hasEnough(Inventory inventory, Inventory needs) {
		for (Inventory item : needs.slots()) {
			if (item.peek().isPresent()) {
				Optional<ItemStack> template = getTemplate(inventory, item.peek().get());
				if (!template.isPresent() || inventory.query(template.get()).totalItems() < item.totalItems())
					return false;
			}
		}
		return true;
	}

	static private boolean equalTo(ItemStack item, ItemStack needle) {
		if (item.equalTo(needle))
			return true;
		for (Entry<DataQuery, Object> pair : needle.toContainer().getValues(true).entrySet()) {
			if (pair.getKey().toString().equals("ItemType")
					|| pair.getKey().toString().equals("UnsafeDamage")
					|| pair.getKey().toString().equals("UnsafeData.ench")
					|| pair.getKey().toString().equals("UnsafeData.StoredEnchantments")
					|| pair.getKey().toString().equals("UnsafeData.Potion")
					|| pair.getKey().toString().equals("UnsafeData.EntityTag")) {
				Optional<Object> other = item.toContainer().get(pair.getKey());
				if (!other.isPresent() || !pair.getValue().toString().equals(other.get().toString())) {
					return false;
				}
			}
		}

		return true;
	}

	static public Optional<ItemStack> getTemplate(Inventory inventory, ItemStack needle) {
		for (Inventory item : inventory.slots()) {
			if (item.peek().isPresent()) {
				if (equalTo(item.peek().get(), needle))
					return item.peek();
			}
		}
		return Optional.empty();
	}

	static public boolean build(Player player, Location<World> target) {
		Optional<TileEntity> sign = target.getTileEntity();
		if (sign.isPresent() && sign.get().supports(SignData.class)) {
			Optional<SignData> data = sign.get().get(SignData.class);
			if (data.isPresent()) {
				SignData signData = data.get();
				Shop shop;
				try {
					boolean needEconomy = false;
					switch (signData.lines().get(0).toPlain().toLowerCase()) {
					case "[itrade]":
						shop = new iTrade(player, target);
						break;
					case "[ibuy]":
						needEconomy = true;
						shop = new iBuy(player, target);
						break;
					case "[isell]":
						needEconomy = true;
						shop = new iSell(player, target);
						break;
					case "[atrade]":
						shop = new aTrade(player, target);
						break;
					case "[abuy]":
						needEconomy = true;
						shop = new aBuy(player, target);
						break;
					case "[asell]":
						needEconomy = true;
						shop = new aSell(player, target);
						break;
					case "[trade]":
						shop = new Trade(player, target);
						break;
					case "[buy]":
						needEconomy = true;
						shop = new Buy(player, target);
						break;
					case "[sell]":
						needEconomy = true;
						shop = new Sell(player, target);
						break;
					case "[bank]":
						needEconomy = true;
						shop = new Bank(player, target);
						break;
					case "[heal]":
						shop = new Heal(player, target);
						break;
					case "[deviceon]":
						shop = new DeviceOn(player, target);
						break;
					case "[deviceoff]":
						shop = new DeviceOff(player, target);
						break;
					case "[toggle]":
						shop = new Toggle(player, target);
						break;
					default:
						return false;
					}
					if (needEconomy && CarrotShop.getEcoService() == null) {
						return false;
					}
				} catch (ExceptionInInitializerError e) {
					player.sendMessage(Text.of(TextColors.DARK_RED, e.getMessage()));
					return false;
				}

				for (Location<World> loc : shop.getLocations()) {
					Optional<List<Shop>> oldShopList = ShopsData.getShops(loc);
					if (oldShopList.isPresent()) {
						for (Shop oldShop : oldShopList.get()) {
							if (!oldShop.isOwner(player)) {
								player.sendMessage(Text.of(TextColors.DARK_RED, "This shop would override a shop you do not own. Abort."));
								return false;
							}
						}
					}
				}

				Optional<List<Shop>> oldShopList = ShopsData.getShops(shop.getLocation());
				if (oldShopList.isPresent()) {
					List<Shop> toDelete = new ArrayList<>();
					oldShopList.get().forEach((oldShop) -> {
						toDelete.add(oldShop);
					});
					toDelete.forEach((oldShop) -> {
						oldShop.destroy(player);
					});
				}
				ShopsData.addShop(shop);
				return true;
			}
		}
		return false;
	}

}
