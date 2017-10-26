package com.carrot.carrotshop.shop;

import java.math.BigDecimal;
import java.util.List;
import java.util.Stack;

import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.carrot.carrotshop.CarrotShop;
import com.carrot.carrotshop.ShopsData;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class DeviceOn extends Shop {
	@Setting
	private Location<World> lever;
	@Setting
	private int price;

	public DeviceOn() {
	}

	public DeviceOn(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		if (!player.hasPermission("carrotshop.create.device"))
			throw new ExceptionInInitializerError("You don't have perms to build a device sign");
		Stack<Location<World>> locations = ShopsData.getItemLocations(player);
		if (locations.isEmpty())
			throw new ExceptionInInitializerError("Device signs require a lever");
		BlockState targetBlock = locations.peek().getBlock();
		if (!targetBlock.getType().equals(BlockTypes.LEVER))
			throw new ExceptionInInitializerError("Device signs require a lever");

		lever = locations.peek();

		if (CarrotShop.getEcoService() != null) {
			price = getPrice(sign);
			if (price < 0)
				throw new ExceptionInInitializerError("bad price");
		}

		ShopsData.clearItemLocations(player);
		player.sendMessage(Text.of(TextColors.DARK_GREEN, "You have setup a device sign:"));
		done(player);
		info(player);
	}

	@Override
	public void info(Player player) {
		if (CarrotShop.getEcoService() != null)
			player.sendMessage(Text.of("Activate for ", formatPrice(price), "?"));
		else
			player.sendMessage(Text.of("Activate?"));
		update();
	}

	@Override
	public List<Location<World>> getLocations() {
		List<Location<World>> locations = super.getLocations();
		locations.add(lever);
		return locations;
	}

	@Override
	public boolean trigger(Player player) {
		if (CarrotShop.getEcoService() != null) {
			UniqueAccount buyerAccount = CarrotShop.getEcoService().getOrCreateAccount(player.getUniqueId()).get();
			TransactionResult result = buyerAccount.withdraw(getCurrency(), BigDecimal.valueOf(price), CarrotShop.getCause());
			if (result.getResult() != ResultType.SUCCESS) {
				player.sendMessage(Text.of(TextColors.DARK_RED, "You don't have enough money!"));
				return false;
			}
			player.sendMessage(Text.of("Device activated for ", formatPrice(price)));
		} else {
			player.sendMessage(Text.of("Device activated"));
		}

		//lever.offer(Keys.POWERED, true, CarrotShop.getCause());



		return true;
	}

}
