package com.carrot.carrotshop.shop;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.carrot.carrotshop.CarrotShop;

import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class Bank extends Shop {

	public Bank() {
	}

	public Bank(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		if (!player.hasPermission("carrotshop.create.bank"))
			throw new ExceptionInInitializerError("You don't have perms to build a bank sign");

		player.sendMessage(Text.of(TextColors.DARK_GREEN, "You have setup an bank shop"));
	}

	@Override
	public void info(Player player) {

		player.sendMessage(Text.of("Right click to see your balance"));

	}
	
	@Override
	public boolean trigger(Player player) {
		UniqueAccount account = CarrotShop.getEcoService().getOrCreateAccount(player.getUniqueId()).get();
		
		player.sendMessage(Text.of("Your balance: ", account.getBalance(CarrotShop.getEcoService().getDefaultCurrency()), " ", CarrotShop.getEcoService().getDefaultCurrency().getPluralDisplayName()));

		return true;
	}

}
