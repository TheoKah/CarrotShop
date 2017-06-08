package com.carrot.carrotshop;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class ShopsLogs {
	private static File carrotLogsFolder;

	public static void init(File rootDir) throws IOException
	{
		carrotLogsFolder = new File(rootDir, "logs");
		carrotLogsFolder.mkdirs();
	}

	public static Optional<String> getLog(UUID shopOwner) {
		File file = getFile(shopOwner);
		if (!file.exists())
			return Optional.empty();

		StringBuilder data = new StringBuilder();
		data.append("{\"info\":{");

		data.append("\"time\":{\".sv\":\"timestamp\"}");

		Optional<UserStorageService> userStorage = Sponge.getServiceManager().provide(UserStorageService.class);

		Optional<User> player = userStorage.get().get(shopOwner);
		if (!player.isPresent())
			return Optional.empty();
		data.append(",\"player\":\"" + player.get().getName() + "\"");
		data.append(",\"playerID\":\"" + player.get().getUniqueId().toString() + "\"");

		if (CarrotShop.getEcoService() != null) {
			data.append(",\"currencyID\":\"" + CarrotShop.getEcoService().getDefaultCurrency().getId() + "\"");
			data.append(",\"currencySymbol\":\"" + CarrotShop.getEcoService().getDefaultCurrency().getSymbol().toPlain() + "\"");
			data.append(",\"currencyName\":\"" + CarrotShop.getEcoService().getDefaultCurrency().getName() + "\"");
			data.append(",\"currencyDName\":\"" + CarrotShop.getEcoService().getDefaultCurrency().getDisplayName() + "\"");
			data.append(",\"currencyPDName\":\"" + CarrotShop.getEcoService().getDefaultCurrency().getPluralDisplayName() + "\"");
		}
		
		
		data.append("},\"logs\":[");

		try {
			data.append(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
		} catch (IOException e) {
			return Optional.empty();
		}

		data.append("]}");
		return Optional.of(data.toString());
	}

	public static void log(UUID shopOwner, Player player, String type, Location<World> location, Optional<Integer> price, Optional<Inventory> itemsA, Optional<Inventory> itemsB) {

		JsonObject newNode = new JsonObject();
		newNode.addProperty("player", player.getName());
		newNode.addProperty("playerID", player.getUniqueId().toString());
		newNode.addProperty("type", type);
		newNode.addProperty("time", System.currentTimeMillis());

		JsonObject locationNode = new JsonObject();
		locationNode.addProperty("world", location.getExtent().getName());
		locationNode.addProperty("worldID", location.getExtent().getUniqueId().toString());
		locationNode.addProperty("X", location.getBlockX());
		locationNode.addProperty("Y", location.getBlockY());
		locationNode.addProperty("Z", location.getBlockZ());
		newNode.add("location", locationNode);


		if (price.isPresent())
			newNode.addProperty("price", price.get());

		if (itemsA.isPresent())
			newNode.add("items", invToArray(itemsA.get()));

		if (itemsB.isPresent())
			newNode.add("items2", invToArray(itemsB.get()));
		
		try {
			File file = new File(carrotLogsFolder, shopOwner.toString() + ".shoplog");
			
			if (!file.exists()) {
				file.createNewFile();
				JsonObject firstNode = new JsonObject();
				firstNode.addProperty("type", "init");
				firstNode.addProperty("time", System.currentTimeMillis());
				
				Files.write(file.toPath(), firstNode.toString().getBytes(), StandardOpenOption.APPEND);
			}
			Files.write(file.toPath(), ",".getBytes(), StandardOpenOption.APPEND);
			Files.write(file.toPath(), newNode.toString().getBytes(), StandardOpenOption.APPEND);

		} catch (IOException e) {
			CarrotShop.getLogger().error("Unable to store logs for shop " + shopOwner + " triggered by " + player.getName() + ": " + e.getMessage());
		}
	}

	private static File getFile(UUID shopOwner) {
		return new File(carrotLogsFolder, shopOwner.toString() + ".shoplog");
	}

	private static JsonArray invToArray(Inventory inv) {
		JsonArray array = new JsonArray();

		for (Inventory slot : inv.slots()) {
			if (slot.peek().isPresent()) {
				JsonObject item = new JsonObject();
				item.addProperty("id", slot.peek().get().getItem().getId());
				item.addProperty("name", slot.peek().get().getTranslation().get());
				item.addProperty("quantity", slot.peek().get().getQuantity());
				array.add(item);
			}
		}
		return array;
	}

}
