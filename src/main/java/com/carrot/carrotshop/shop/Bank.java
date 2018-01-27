package com.carrot.carrotshop.shop;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.carrot.carrotshop.CarrotShop;
import com.carrot.carrotshop.Lang;

import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class Bank extends Shop {
	
	static private String type = "Bank";

	public Bank() {
	}

	public Bank(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		if (!player.hasPermission("carrotshop.create.bank"))
			throw new ExceptionInInitializerError(Lang.SHOP_PERM.replace("%type%", type));
		player.sendMessage(Text.of(TextColors.DARK_GREEN, Lang.SHOP_DONE.replace("%type%", type)));
		done(player);
	}

	@Override
	public void info(Player player) {

		player.sendMessage(Text.of(Lang.SHOP_BANK_HELP));
		update();
	}
	
	@Override
	public boolean trigger(Player player) {
		UniqueAccount account = CarrotShop.getEcoService().getOrCreateAccount(player.getUniqueId()).get();
		
		player.sendMessage(Text.of(Lang.SHOP_BANK.replace("%bank%", formatPrice(account.getBalance(getCurrency())))));
		
		return true;
	}

}
