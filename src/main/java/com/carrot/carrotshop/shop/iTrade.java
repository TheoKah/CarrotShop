package com.carrot.carrotshop.shop;

import java.util.Optional;
import java.util.Stack;

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

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class iTrade extends Shop {
	@Setting
	private Inventory toGive;
	@Setting
	private Inventory toTake;

	public iTrade() {
	}

	public iTrade(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		if (!player.hasPermission("carrotshop.admin"))
			throw new ExceptionInInitializerError("You don't have perms to build an iTrade sign");
		Stack<Location<World>> locations = ShopsData.getItemLocations(player);
		if (locations.size() < 2)
			throw new ExceptionInInitializerError("iTrade signs require you setup two chests");
		Optional<TileEntity> chestTakeOpt = locations.get(0).getTileEntity();
		Optional<TileEntity> chestGiveOpt = locations.get(1).getTileEntity();
		if (!chestTakeOpt.isPresent() || ! chestGiveOpt.isPresent() || !(chestTakeOpt.get() instanceof TileEntityCarrier) || !(chestGiveOpt.get() instanceof TileEntityCarrier))
			throw new ExceptionInInitializerError("iTrade signs require you setup two chests");
		Inventory chestTake = ((TileEntityCarrier) chestTakeOpt.get()).getInventory();
		Inventory chestGive = ((TileEntityCarrier) chestGiveOpt.get()).getInventory();
		if (chestTake.totalItems() == 0 || chestGive.totalItems() == 0)
			throw new ExceptionInInitializerError("chest cannot be empty");
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
		ShopsData.clearItemLocations(player);
		player.sendMessage(Text.of(TextColors.DARK_GREEN, "You have setup an iTrade shop:"));
		info(player);
	}

	@Override
	public void info(Player player) {
		Builder builder = Text.builder();
		builder.append(Text.of("Trade"));
		for (Inventory item : toTake.slots()) {
			if (item.peek().isPresent()) {
				builder.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getItem().getTranslation().get(), " x", item.peek().get().getQuantity()));
			}
		}
		builder.append(Text.of(" and get"));
		for (Inventory item : toGive.slots()) {
			if (item.peek().isPresent()) {
				builder.append(Text.of(TextColors.YELLOW, " ", item.peek().get().getItem().getTranslation().get(), " x", item.peek().get().getQuantity()));
			}
		}
		builder.append(Text.of("?"));
		player.sendMessage(builder.build());
		update();

	}
	@Override
	public boolean trigger(Player player) {
		if (!hasEnough(player.getInventory(), toTake)) {
			player.sendMessage(Text.of(TextColors.DARK_RED, "You are missing items for the trade!"));
			return false;
		}
		Inventory inv = player.getInventory().query(InventoryRow.class);
		for (Inventory item : toTake.slots()) {
			if (item.peek().isPresent()) {
				Optional<ItemStack> template = getTemplate(inv, item.peek().get());
				if (template.isPresent()) {
					inv.query(template.get()).poll(item.peek().get().getQuantity());
				}
			}
		}
		for (Inventory item : toGive.slots()) {
			if (item.peek().isPresent()) {
				inv.offer(item.peek().get().copy()).getRejectedItems().forEach(action -> {
					putItemInWorld(action, player.getLocation());
				});
			}
		}
		return true;
	}

}
