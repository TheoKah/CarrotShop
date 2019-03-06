package com.carrot.carrotshop.shop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.carrot.carrotshop.CarrotShop;
import com.carrot.carrotshop.Lang;
import com.carrot.carrotshop.ShopsData;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class Cmd extends Shop {
	@Setting
	private String id;
	@Setting
	private float price;
	
	static private String type = "Cmd";

	public Cmd() {
	}

	public Cmd(Player player, Location<World> sign) throws ExceptionInInitializerError {
		super(sign);
		if (!player.hasPermission("carrotshop.admin.cmd"))
			throw new ExceptionInInitializerError(Lang.SHOP_PERM.replace("%type%", type));

		id = sign.getExtent().getName() + "_" + sign.getBlockX() + "_" + sign.getBlockY() + "_"+ sign.getBlockY();
		File cmdFile = new File(ShopsData.getCmdsDirs(), id + ".txt");
		try {
			cmdFile.createNewFile();
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
		
		if (CarrotShop.getEcoService() != null) {
			price = getPrice(sign);
			if (price < 0)
				throw new ExceptionInInitializerError(Lang.SHOP_PRICE);
		}
		player.sendMessage(Text.of(TextColors.DARK_GREEN, Lang.SHOP_DONE.replace("%type%", type)));
		player.sendMessage(Text.of(TextColors.DARK_GREEN, Lang.SHOP_CMD_DONE.replace("%id%", id.toString())));
		CarrotShop.getLogger().info("Cmd file can be found in " + cmdFile.getAbsolutePath());
		done(player);
	}

	@Override
	public void info(Player player) {
		if (CarrotShop.getEcoService() != null)
			player.sendMessage(Text.of(Lang.SHOP_CMD_HELP.replace("%price%", formatPrice(price))));
		else
			player.sendMessage(Text.of(Lang.SHOP_CMD_HELP_NOECON));
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
		}
		
		File cmdFile = new File(ShopsData.getCmdsDirs(), id + ".txt");
		
		try (BufferedReader br = new BufferedReader(new FileReader(cmdFile))) {
			for(String line; (line = br.readLine()) != null; ) {
		       Sponge.getCommandManager().process(Sponge.getServer().getConsole(), line.replace("%player%", player.getName()));
		    }
		} catch (FileNotFoundException e) {
			CarrotShop.getLogger().info("Cmd file not found: " + cmdFile.getAbsolutePath());
			player.sendMessage(Text.of(TextColors.DARK_RED, Lang.SHOP_CMD_ERROR_FILE404));
			return false;
		} catch (IOException e) {
			CarrotShop.getLogger().info("Error with cmd file: " + cmdFile.getAbsolutePath());
			player.sendMessage(Text.of(TextColors.DARK_RED, Lang.SHOP_CMD_ERROR));
			e.printStackTrace();
			return false;
		}
		
		if (CarrotShop.getEcoService() != null)
			player.sendMessage(Text.of(Lang.SHOP_CMD.replace("%price%", formatPrice(price))));
		else
			player.sendMessage(Text.of(Lang.SHOP_CMD_NOECON));
		return true;
	}

}
