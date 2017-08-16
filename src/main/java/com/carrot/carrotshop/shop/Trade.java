package com.carrot.carrotshop.shop;

import java.util.List;
import java.util.Optional;
import java.util.Stack;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.type.InventoryRow;
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
public class Trade extends Shop {
	@Setting
	private Inventory toGive;
	@Setting
	private Inventory toTake;
	@Setting
	private Location<World> toGiveChest;
	@Setting
	private Location<World> toTakeChest;

	public Trade() {
	}

	public Trade(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		if (!player.hasPermission("carrotshop.create.trade"))
			throw new ExceptionInInitializerError("You don't have perms to build a Trade sign");
		Stack<Location<World>> locations = ShopsData.getItemLocations(player);
		if (locations.size() < 2)
			throw new ExceptionInInitializerError("Trade signs require you setup two chests");
		Optional<TileEntity> chestTakeOpt = locations.get(0).getTileEntity();
		Optional<TileEntity> chestGiveOpt = locations.get(1).getTileEntity();
		if (!chestTakeOpt.isPresent() || ! chestGiveOpt.isPresent() || !(chestTakeOpt.get() instanceof TileEntityCarrier) || !(chestGiveOpt.get() instanceof TileEntityCarrier))
			throw new ExceptionInInitializerError("Trade signs require you setup two chests");
		Inventory chestTake = ((TileEntityCarrier) chestTakeOpt.get()).getInventory();
		Inventory chestGive = ((TileEntityCarrier) chestGiveOpt.get()).getInventory();
		if (chestTake.totalItems() == 0 || chestGive.totalItems() == 0)
			throw new ExceptionInInitializerError("chest cannot be empty");
		toTakeChest = locations.get(0);
		toGiveChest = locations.get(1);
		toTake = Inventory.builder().from(chestTake).build(CarrotShop.getInstance());
		toGive = Inventory.builder().from(chestGive).build(CarrotShop.getInstance());
		for(Inventory item : chestGive.slots()) {
			if (item.peek().isPresent())
				toGive.offer(item.peek().get());
		}
		for(Inventory item : chestTake.slots()) {
			if (item.peek().isPresent())
				toTake.offer(item.peek().get());
		}
		setOwner(player);
		ShopsData.clearItemLocations(player);
		player.sendMessage(Text.of(TextColors.DARK_GREEN, "You have setup a Trade shop:"));
		info(player);
	}

	@Override
	public List<Location<World>> getLocations() {
		List<Location<World>> locations = super.getLocations();
		locations.add(toGiveChest);
		locations.add(toTakeChest);
		return locations;
	}

	@Override
	public boolean update() {
		Optional<TileEntity> chest = toGiveChest.getTileEntity();
		if (chest.isPresent() && chest.get() instanceof TileEntityCarrier) {
			if (!hasEnough(((TileEntityCarrier) chest.get()).getInventory(), toGive)) {
				setFail();
				return false;
			}
		} else {
			setFail();
			return false;
		}
		chest = toTakeChest.getTileEntity();
		if (chest.isPresent() && chest.get() instanceof TileEntityCarrier) {
			Inventory chestInv = ((TileEntityCarrier) chest.get()).getInventory();
			if (chestInv.capacity() - chestInv.size() < toTake.size()) {
				setFail();
				return false;
			}
		} else {
			setFail();
			return false;
		}

		setOK();
		return true;
	}

	@Override
	public void info(Player player) {
		Builder builder = Text.builder();
		builder.append(Text.of("Trade"));
		for (Inventory item : toTake.slots()) {
			if (item.peek().isPresent()) {
				builder.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
			}
		}
		builder.append(Text.of(" and get"));
		for (Inventory item : toGive.slots()) {
			if (item.peek().isPresent()) {
				builder.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
			}
		}
		builder.append(Text.of("?"));
		player.sendMessage(builder.build());
		if (!update())
			player.sendMessage(Text.of(TextColors.GOLD, "This shop is either full or empty!"));

	}
	@Override
	public boolean trigger(Player player) {
		Optional<TileEntity> chestToGive = toGiveChest.getTileEntity();
		if (chestToGive.isPresent() && chestToGive.get() instanceof TileEntityCarrier) {
			if (!hasEnough(((TileEntityCarrier) chestToGive.get()).getInventory(), toGive)) {
				player.sendMessage(Text.of(TextColors.GOLD, "This shop is empty!"));
				update();
				return false;
			}
		} else {
			return false;
		}
		Optional<TileEntity> chestToTake = toTakeChest.getTileEntity();
		if (!chestToTake.isPresent() || !(chestToTake.get() instanceof TileEntityCarrier)) {
			return false;
		}
		Optional<TileEntity> chest = toTakeChest.getTileEntity();
		if (chest.isPresent() && chest.get() instanceof TileEntityCarrier) {
			Inventory chestInv = ((TileEntityCarrier) chest.get()).getInventory();
			if (chestInv.capacity() - chestInv.size() < toTake.size()) {
				player.sendMessage(Text.of(TextColors.GOLD, "This shop is full!"));
				update();
				return false;
			}
		}
		
		Inventory inv = player.getInventory().query(InventoryRow.class);

		if (!hasEnough(inv, toTake)) {
			player.sendMessage(Text.of(TextColors.DARK_RED, "You are missing items for the trade!"));
			return false;
		}
		
		Inventory invToTake = ((TileEntityCarrier) chestToTake.get()).getInventory();
		Inventory invToGive = ((TileEntityCarrier) chestToGive.get()).getInventory();

		Builder itemsName = Text.builder();
		for (Inventory item : toTake.slots()) {
			if (item.peek().isPresent()) {
				Optional<ItemStack> template = getTemplate(inv, item.peek().get());
				if (template.isPresent()) {
					itemsName.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
					Optional<ItemStack> items = inv.query(template.get()).poll(item.peek().get().getQuantity());
					if (items.isPresent()) {
						invToTake.offer(items.get());
					} else {
						return false;
					}
				}
			}
		}

		itemsName.append(Text.of(" for"));

		for (Inventory item : toGive.slots()) {
			if (item.peek().isPresent()) {
				Optional<ItemStack> template = getTemplate(invToGive, item.peek().get());
				if (template.isPresent()) {
					itemsName.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getTranslation().get(), " x", item.peek().get().getQuantity()));
					Optional<ItemStack> items = invToGive.query(template.get()).poll(item.peek().get().getQuantity());
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

		ShopsLogs.log(getOwner(), player, "trade", super.getLocations().get(0), Optional.empty(), Optional.of(toGive), Optional.of(toTake));

		Text report = Text.of(" traded", itemsName.build());

		player.sendMessage(Text.of("You", report));

		if (!CarrotShop.noSpam(getOwner())) {
			Optional<Player> seller = Sponge.getServer().getPlayer(getOwner());
			if (seller.isPresent()) {
				seller.get().sendMessage(Text.of(player.getName(), report));
			}
		}

		update();
		return true;
	}

}
