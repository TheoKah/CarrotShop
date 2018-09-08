package com.carrot.carrotshop;


import java.io.File;
import java.io.IOException;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

public class ShopConfig
{
	private static File configFile;
	private static ConfigurationLoader<CommentedConfigurationNode> configManager;
	private static CommentedConfigurationNode config;

	public static void init(File rootDir)
	{
		configFile = new File(rootDir, "config.conf");
		configManager = HoconConfigurationLoader.builder().setPath(configFile.toPath()).build();
	}

	public static void load()
	{
		try
		{
			if (!configFile.exists())
			{
				configFile.getParentFile().mkdirs();
				configFile.createNewFile();
				config = configManager.load();
				configManager.save(config);
			}
			config = configManager.load();
		}
		catch (IOException e)
		{
			CarrotShop.getLogger().error(Lang.CMD_CONFIG_RELOAD_FILE);
			e.printStackTrace();
			return;
		}
		

		// check integrity

		config.getNode("taxes").setComment("Percentage of the displayed price that will not be given to shop owner. Note that this option might not work well if you are using special economy plugins such as the ones that use items as currency");
		Utils.ensurePositiveNumber(config.getNode("taxes", "Buy"), 0);
		Utils.ensurePositiveNumber(config.getNode("taxes", "Sell"), 0);
		Utils.ensurePositiveNumber(config.getNode("taxes", "DeviceOn"), 0);
		Utils.ensurePositiveNumber(config.getNode("taxes", "DeviceOff"), 0);
		Utils.ensurePositiveNumber(config.getNode("taxes", "Toggle"), 0);

		config.getNode("cost").setComment("Cost for creating a sign");
		Utils.ensurePositiveNumber(config.getNode("cost", "Bank"), 0);
		Utils.ensurePositiveNumber(config.getNode("cost", "Buy"), 0);
		Utils.ensurePositiveNumber(config.getNode("cost", "Sell"), 0);
		Utils.ensurePositiveNumber(config.getNode("cost", "DeviceOn"), 0);
		Utils.ensurePositiveNumber(config.getNode("cost", "DeviceOff"), 0);
		Utils.ensurePositiveNumber(config.getNode("cost", "Toggle"), 0);
		Utils.ensurePositiveNumber(config.getNode("cost", "Trade"), 0);

		config.getNode("others", "emptyhand").setComment("If true, using signs require empty hands, see https://github.com/TheoKah/CarrotShop/issues/30");
		Utils.ensureBoolean(config.getNode("others", "emptyhand"), false);
		
		save();
	}

	public static void save()
	{
		try
		{
			configManager.save(config);
		}
		catch (IOException e)
		{
			CarrotShop.getLogger().error("Could not save config file !");
		}
	}

	public static CommentedConfigurationNode getNode(String... path)
	{
		return config.getNode((Object[]) path);
	}

	public static class Utils
	{
		public static void ensureString(CommentedConfigurationNode node, String def)
		{
			if (node.getString() == null)
			{
				node.setValue(def);
			}
		}

		public static void ensurePositiveNumber(CommentedConfigurationNode node, Number def)
		{
			if (!(node.getValue() instanceof Number) || node.getDouble(-1) < 0)
			{
				node.setValue(def);
			}
		}

		public static void ensureBoolean(CommentedConfigurationNode node, boolean def)
		{
			if (!(node.getValue() instanceof Boolean))
			{
				node.setValue(def);
			}
		}
	}
}
