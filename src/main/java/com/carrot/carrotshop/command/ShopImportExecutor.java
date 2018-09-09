package com.carrot.carrotshop.command;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.text.Text;

import com.carrot.carrotshop.CarrotShop;
import com.carrot.carrotshop.Lang;
import com.carrot.carrotshop.ShopsData;
import com.carrot.carrotshop.command.importObjects.AdminShop;
import com.carrot.carrotshop.shop.Shop;
import com.carrot.carrotshop.shop.iBuy;
import com.carrot.carrotshop.shop.iSell;
import com.google.common.reflect.TypeToken;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class ShopImportExecutor implements CommandExecutor{
	File defaultConfigDir;
	
	public ShopImportExecutor(File defaultConfigDir) {
		this.defaultConfigDir = defaultConfigDir;
	}

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		Optional<String> plugin = args.<String>getOne("plugin");
		
		if (!plugin.isPresent()) {
			src.sendMessage(Text.of(Lang.CMD_IMPORT_USAGE));
			src.sendMessage(Text.of(" - adminshop"));
			return CommandResult.empty();
		}
		if (plugin.get().equalsIgnoreCase("adminshop")) {
			return importAdminShop(src, new File(defaultConfigDir, "adminshop/data/shops.conf"));
		} else {
			src.sendMessage(Text.of(Lang.CMD_IMPORT_USAGE));
			src.sendMessage(Text.of(" - adminshop"));
			return CommandResult.empty();
		}
	}
	
	private CommandResult importAdminShop(CommandSource src, File file) {
		if (!file.exists() || !file.canRead()) {
			src.sendMessage(Text.of(Lang.CMD_IMPORT_ADMINSHOP_ERROR_FILE));
			return CommandResult.empty();
		}
		
		int count = 0;
		
		try {
			ConfigurationNode shopsNode = HoconConfigurationLoader.builder().setFile(file).build().load();
			for (ConfigurationNode shopNode : shopsNode.getNode("shops").getChildrenMap().values()) {
				try {
					AdminShop adminshop = (AdminShop) shopNode.getValue(TypeToken.of(AdminShop.class));
					
					Optional<TileEntity> sign = adminshop.signLocation.getTileEntity();
					if (sign.isPresent() && sign.get().supports(SignData.class)) {
						Optional<SignData> data = sign.get().get(SignData.class);
						if (data.isPresent()) {
							SignData signData = data.get();
							if (adminshop.buyShop)
								signData.set(signData.lines().set(0, Text.of("[iSell]")));
							else
								signData.set(signData.lines().set(0, Text.of("[iBuy]")));
							String itemName = Shop.getItemName(adminshop.item.createStack()).toPlain();
							if (itemName.length() > 15)
								signData.set(signData.lines().set(1, Text.of(itemName.substring(12), "...")));
							else
								signData.set(signData.lines().set(1, Text.of(itemName)));
							signData.set(signData.lines().set(2, Text.of("x", adminshop.item.getCount())));
							signData.set(signData.lines().set(3, Text.EMPTY));
							if (adminshop.price > 0 && CarrotShop.getEcoService() != null)
								signData.set(signData.lines().set(3, Text.of(ShopsData.getCurrency().format(BigDecimal.valueOf(adminshop.price)))));
							sign.get().offer(signData);
							
							Shop shop;
							if (adminshop.buyShop)
								shop = new iSell(adminshop.signLocation, adminshop.item);
							else
								shop = new iBuy(adminshop.signLocation, adminshop.item);
							
							Optional<List<Shop>> oldShopList = ShopsData.getShops(shop.getLocation());
							if (oldShopList.isPresent()) {
								List<Shop> toDelete = new ArrayList<>();
								oldShopList.get().forEach((oldShop) -> {
									toDelete.add(oldShop);
								});
								toDelete.forEach((oldShop) -> {
									oldShop.destroy(src);
								});
							}
							ShopsData.addShop(shop);
						}
					}
				} catch (ObjectMappingException e) {
					src.sendMessage(Text.of(Lang.CMD_IMPORT_ADMINSHOP_ERROR_LOAD_ITEM));
					src.sendMessage(Text.of(shopNode.getKey().toString()));
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			src.sendMessage(Text.of(Lang.CMD_IMPORT_ADMINSHOP_ERROR_LOAD));
			e.printStackTrace();
		}
		
		return CommandResult.queryResult(count);
	}
	
}
