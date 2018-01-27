package com.carrot.carrotshop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;

import com.carrot.carrotshop.command.NoSpamExecutor;
import com.carrot.carrotshop.command.ShopConfigCurrencyExecutor;
import com.carrot.carrotshop.command.ShopConfigExecutor;
import com.carrot.carrotshop.command.ShopMainExecutor;
import com.carrot.carrotshop.command.ShopReportExecutor;
import com.carrot.carrotshop.command.ShopServerReportExecutor;
import com.carrot.carrotshop.command.ShopWikiExecutor;
import com.carrot.carrotshop.command.element.CurrencyElement;
import com.carrot.carrotshop.listener.BlockBreakListener;
import com.carrot.carrotshop.listener.PlayerClickListener;
import com.carrot.carrotshop.listener.PlayerConnexionListener;
import com.google.inject.Inject;

@Plugin(id = "carrotshop", name = "CarrotShop", authors={"Carrot"}, url="https://github.com/TheoKah/CarrotShop")
public class CarrotShop {
	private File rootDir;

	private static CarrotShop plugin;

	@Inject
	private Logger logger;

	@Inject
	@ConfigDir(sharedRoot = true)
	private File defaultConfigDir;

	private EconomyService economyService = null;

	private static List<UUID> noSpam = new ArrayList<UUID>();

	@Listener
	public void onInit(GameInitializationEvent event) throws IOException
	{
		plugin = this;

		rootDir = new File(defaultConfigDir, "carrotshop");

		ShopsLogs.init(rootDir);
		ShopsData.init(rootDir);
	}

	@Listener
	public void onStart(GameStartedServerEvent event)
	{
		Sponge.getServiceManager()
		.getRegistration(EconomyService.class)
		.ifPresent(prov -> economyService = prov.getProvider());
		
		ShopsData.load();

		CommandSpec shopReport = CommandSpec.builder()
				.description(Text.of("Generare a CarrotShop report"))
				.executor(new ShopReportExecutor())
				.arguments(GenericArguments.optional(GenericArguments.user(Text.of("player"))))
				.build();
		
		CommandSpec shopServerReport = CommandSpec.builder()
				.description(Text.of("Generare a CarrotShop report for iSigns"))
				.executor(new ShopServerReportExecutor())
				.build();
		
		CommandSpec shopSpam = CommandSpec.builder()
				.description(Text.of("Toggle shop message when someone use your shop"))
				.executor(new NoSpamExecutor())
				.build();
		
		CommandSpec shopWiki = CommandSpec.builder()
				.description(Text.of("Displays a link to the shop wiki"))
				.executor(new ShopWikiExecutor())
				.build();
		
		CommandSpec shopConfigCurrency = CommandSpec.builder()
				.description(Text.of("Get/Set the default currency"))
				.permission("carrotshop.config.currency")
				.executor(new ShopConfigCurrencyExecutor())
				.arguments(GenericArguments.optional(new CurrencyElement(Text.of("currency"))))
				.build();
		
		CommandSpec shopConfig = CommandSpec.builder()
				.description(Text.of("Change config of the plugin"))
				.executor(new ShopConfigExecutor())
				.child(shopConfigCurrency, "currency")
				.build();
		
		CommandSpec shopMain = CommandSpec.builder()
				.description(Text.of("Main CarrotShop command"))
				.executor(new ShopMainExecutor())
				.child(shopWiki, "help", "?", "wiki", "how", "howto", "h")
				.child(shopSpam, "hide", "shopchat", "stop", "off", "nospam", "spam", "toggle", "togglechat", "t")
				.child(shopReport, "report", "shopreport", "r")
				.child(shopServerReport, "serverreport", "server", "servreport", "sr")
				.child(shopConfig, "config")
				.build();

		Sponge.getCommandManager().register(plugin, shopReport, "shopreport", "carrotshopreport", "cr", "sr", "carrotreport");
		Sponge.getCommandManager().register(plugin, shopServerReport, "serverreport", "carrotshopserverreport", "shopserverreport", "carrotserverreport", "csr", "ssr", "csreport", "ssreport");
		Sponge.getCommandManager().register(plugin, shopSpam, "shophide", "hideshopchat", "carrotshophide", "carrothide", "shide", "chide", "sh", "ch");
		Sponge.getCommandManager().register(plugin, shopWiki, "shophelp", "carrotshopwiki", "shophelp", "carrotshopwiki", "cshophelp", "cshopwiki", "carrothelp", "carrotwiki", "shelp", "swiki");
		Sponge.getCommandManager().register(plugin, shopMain, "shop", "cs", "carrotshop", "s", "c");

		Sponge.getEventManager().registerListeners(this, new PlayerClickListener());
		Sponge.getEventManager().registerListeners(this, new BlockBreakListener());
		Sponge.getEventManager().registerListeners(this, new PlayerConnexionListener());
	}

	@Listener
	public void onStop(GameStoppingServerEvent event) {
		ShopsData.unload();
	}

	public static CarrotShop getInstance()
	{
		return plugin;
	}

	public static Logger getLogger()
	{
		return getInstance().logger;
	}

	public static EconomyService getEcoService()
	{
		return getInstance().economyService;
	}

	public static Cause getCause()
	{
		return Cause.source(Sponge.getPluginManager().fromInstance(CarrotShop.getInstance()).get()).build();
	}

	public static boolean noSpam(UUID uuid) {
		return noSpam.contains(uuid);
	}

	public static boolean toggleSpam(UUID uuid) {
		if (noSpam.contains(uuid))
			noSpam.remove(uuid);
		else
			noSpam.add(uuid);
		return noSpam.contains(uuid);
	}
}
