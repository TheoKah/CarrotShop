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

	public List<Location<World>> getLocations() {
		List<Location<World>> locations = new ArrayList<>();
		locations.add(location);
		return locations;
	}

	protected final void setOwner(Player player) {
		player.sendMessage(Text.of("set as owner"));
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
					return -1;
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
					|| pair.getKey().toString().equals("UnsafeData.StoredEnchantments")
					|| pair.getKey().toString().equals("UnsafeData.Potion")) {
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
					Optional<Shop> oldShop = ShopsData.getShop(loc);
					if (oldShop.isPresent()) {
						if (!oldShop.get().destroy(player)) {
							player.sendMessage(Text.of(TextColors.DARK_RED, "This shop would override a shop you do not own. Abort."));
							for (Location<World> loc2 : shop.getLocations()) {
								Optional<Shop> oldShop2 = ShopsData.getShop(loc2);
								if (oldShop.isPresent())
									oldShop2.get().update();
							}
							return false;
						}
					}
				}
				for (Location<World> loc : shop.getLocations()) {
					Optional<Shop> oldShop = ShopsData.getShop(loc);
					if (oldShop.isPresent()) {
						ShopsData.delShop(oldShop.get());
					}
				}
				ShopsData.addShop(shop);
				return true;
			}
		}
		return false;
	}

}
