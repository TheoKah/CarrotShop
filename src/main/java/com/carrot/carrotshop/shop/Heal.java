package com.carrot.carrotshop.shop;

import java.math.BigDecimal;

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
import com.carrot.carrotshop.ShopConfig;
import com.carrot.carrotshop.Lang;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class Heal extends Shop {
	@Setting
	private int price;
	
	static private String type = "Heal";

	public Heal() {
	}

	public Heal(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		if (!player.hasPermission("carrotshop.admin.heal"))
			throw new ExceptionInInitializerError(Lang.SHOP_PERM.replace("%type%", type));

		int cost = 0;
		if (CarrotShop.getEcoService() != null) {
			price = getPrice(sign);
			if (price < 0)
				throw new ExceptionInInitializerError(Lang.SHOP_PRICE);
			cost = ShopConfig.getNode("cost", type).getInt(0);
			if (cost > 0) {
				UniqueAccount buyerAccount = CarrotShop.getEcoService().getOrCreateAccount(player.getUniqueId()).get();
				TransactionResult result = buyerAccount.withdraw(getCurrency(), BigDecimal.valueOf(cost), CarrotShop.getCause());
				if (result.getResult() != ResultType.SUCCESS)
					throw new ExceptionInInitializerError(Lang.SHOP_COST.replace("%type%", type).replace("%cost%", getCurrency().format(BigDecimal.valueOf(cost), 0).toPlain()));
			}
		}
		if (cost > 0)
			player.sendMessage(Text.of(TextColors.DARK_GREEN, Lang.SHOP_DONE_COST.replace("%type%", type).replace("%cost%", getCurrency().format(BigDecimal.valueOf(cost), 0).toPlain())));
		else
			player.sendMessage(Text.of(TextColors.DARK_GREEN, Lang.SHOP_DONE.replace("%type%", type)));
		done(player);
	}

	@Override
	public void info(Player player) {
		if (CarrotShop.getEcoService() != null)
			player.sendMessage(Text.of(Lang.SHOP_HEAL_HELP.replace("%price%", formatPrice(price))));
		else
			player.sendMessage(Text.of(Lang.SHOP_HEAL_HELP_NOECON));
		update();

	}

	@Override
	public boolean trigger(Player player) {
		if (CarrotShop.getEcoService() != null) {
			UniqueAccount buyerAccount = CarrotShop.getEcoService().getOrCreateAccount(player.getUniqueId()).get();
			TransactionResult result = buyerAccount.withdraw(getCurrency(), BigDecimal.valueOf(price), CarrotShop.getCause());
			if (result.getResult() != ResultType.SUCCESS) {
				player.sendMessage(Text.of(TextColors.DARK_RED, Lang.SHOP_MONEY));
				return false;
			}
			player.sendMessage(Text.of(Lang.SHOP_HEAL.replace("%price%", formatPrice(price))));
		}
		else
			player.sendMessage(Text.of(Lang.SHOP_HEAL_NOECON));

		player.offer(Keys.HEALTH, player.get(Keys.MAX_HEALTH).get());

		return true;
	}

}
