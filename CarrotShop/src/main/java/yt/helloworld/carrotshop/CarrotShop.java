package yt.helloworld.carrotshop;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.economy.EconomyService;

import com.google.inject.Inject;

import yt.helloworld.carrotshop.listener.BlockBreakListener;
import yt.helloworld.carrotshop.listener.PlayerClickListener;

@Plugin(id = "carrotshop", name = "CarrotShop", version = "1.0", authors={"Carrot"}, description = "A SignShop-like shop plugin for Sponge.", url="https://github.com/TheoKah/CarrotShop")
public class CarrotShop {
	private File rootDir;

	private static CarrotShop plugin;

	@Inject
	private Logger logger;

	@Inject
	@ConfigDir(sharedRoot = true)
	private File defaultConfigDir;

	private EconomyService economyService = null;

	@Listener
	public void onInit(GameInitializationEvent event) throws IOException
	{
		plugin = this;

		rootDir = new File(defaultConfigDir, "carrotshop");
		
		ShopsData.init(rootDir);
	}

	@Listener
	public void onStart(GameStartedServerEvent event)
	{
		ShopsData.load();
		
		Sponge.getServiceManager()
		.getRegistration(EconomyService.class)
		.ifPresent(prov -> economyService = prov.getProvider());

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
}
