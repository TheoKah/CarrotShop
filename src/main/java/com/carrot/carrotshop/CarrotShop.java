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
import com.carrot.carrotshop.command.ShopReportExecutor;
import com.carrot.carrotshop.command.element.PlayerCmdElement;
import com.carrot.carrotshop.listener.BlockBreakListener;
import com.carrot.carrotshop.listener.PlayerClickListener;
import com.google.inject.Inject;

@Plugin(id = "carrotshop", name = "CarrotShop", version = "1.7", authors={"Carrot"}, description = "A SignShop-like shop plugin for Sponge.", url="https://github.com/TheoKah/CarrotShop")
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
		ShopsData.load();

		Sponge.getServiceManager()
		.getRegistration(EconomyService.class)
		.ifPresent(prov -> economyService = prov.getProvider());

		CommandSpec shopReport = CommandSpec.builder()
				.description(Text.of("Generare a CarrotShop report"))
				.executor(new ShopReportExecutor())
				.arguments(GenericArguments.optional(new PlayerCmdElement(Text.of("player"))))
				.build();

		Sponge.getCommandManager().register(plugin, shopReport, "carrotshopreport", "shopreport", "carrotreport", "cr", "sr", "creport", "sreport");
		
		CommandSpec shopSpam = CommandSpec.builder()
				.description(Text.of("Toggle shop message when someone use your shop"))
				.executor(new NoSpamExecutor())
				.build();

		Sponge.getCommandManager().register(plugin, shopSpam, "hideshopchat", "shophide", "carrothide", "shide", "chide", "sh", "ch");

		Sponge.getEventManager().registerListeners(this, new PlayerClickListener());
		Sponge.getEventManager().registerListeners(this, new BlockBreakListener());
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
		return Cause.source(CarrotShop.getInstance()).build();
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
