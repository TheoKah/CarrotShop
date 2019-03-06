package com.carrot.carrotshop;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.carrot.carrotshop.shop.Shop;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class ShopsLogs {
	private static File carrotLogsFolder;

	public static void init(File rootDir) throws IOException
	{
		carrotLogsFolder = new File(rootDir, "logs");
		carrotLogsFolder.mkdirs();
	}

	public static Optional<String> getLog(UUID shopOwnerUUID) {
		String shopOwner = shopOwnerUUID != null ? shopOwnerUUID.toString() : "server";

		File fileBuy = new File(carrotLogsFolder, shopOwner + ".buy");
		File fileSell = new File(carrotLogsFolder, shopOwner + ".sell");
		File fileTrade = new File(carrotLogsFolder, shopOwner + ".trade");
		if (!fileBuy.exists() && !fileSell.exists() && !fileTrade.exists())
			return Optional.empty();

		StringBuilder data = new StringBuilder();
		data.append("{\"info\":{");

		data.append("\"dbtime\":{\".sv\":\"timestamp\"}");
		data.append(",\"time\":" + System.currentTimeMillis());

		Optional<UserStorageService> userStorage = Sponge.getServiceManager().provide(UserStorageService.class);

		if (shopOwnerUUID != null) {
			Optional<User> player = userStorage.get().get(shopOwnerUUID);
			if (!player.isPresent())
				return Optional.empty();
			data.append(",\"player\":\"" + player.get().getName() + "\"");
			data.append(",\"playerID\":\"" + player.get().getUniqueId().toString() + "\"");
		}
		data.append(",\"motdplain\":\"" + Sponge.getServer().getMotd().toPlain() + "\"");
		data.append(",\"motd\":\"" + TextSerializers.FORMATTING_CODE.serialize(Sponge.getServer().getMotd()) + "\"");

		if (CarrotShop.getEcoService() != null) {
			data.append(",\"currencySymbol\":\"" + TextSerializers.FORMATTING_CODE.serialize(ShopsData.getCurrency().getSymbol()) + "\"");
			data.append(",\"currencySymbolplain\":\"" + ShopsData.getCurrency().getSymbol().toPlain() + "\"");
			data.append(",\"currencyName\":\"" + ShopsData.getCurrency().getName() + "\"");
			data.append(",\"currencyDName\":\"" + TextSerializers.FORMATTING_CODE.serialize(ShopsData.getCurrency().getDisplayName()) + "\"");
			data.append(",\"currencyPDName\":\"" + TextSerializers.FORMATTING_CODE.serialize(ShopsData.getCurrency().getPluralDisplayName()) + "\"");
		}

		data.append("},\"logs\":{");



		data.append("\"buy\":[");
		if (fileBuy.exists()) {
			try {
				data.append(new String(Files.readAllBytes(fileBuy.toPath()), StandardCharsets.UTF_8));
			} catch (IOException e) {
				return Optional.empty();
			}
		}
		data.append("],");


		data.append("\"sell\":[");
		if (fileSell.exists()) {
			try {
				data.append(new String(Files.readAllBytes(fileSell.toPath()), StandardCharsets.UTF_8));
			} catch (IOException e) {
				return Optional.empty();
			}
		}
		data.append("],");


		data.append("\"trade\":[");
		if (fileTrade.exists()) {
			try {
				data.append(new String(Files.readAllBytes(fileTrade.toPath()), StandardCharsets.UTF_8));
			} catch (IOException e) {
				return Optional.empty();
			}
		}
		data.append("]}}");
		return Optional.of(data.toString());
	}

	public static void log(UUID shopOwnerUUID, Player player, String type, Location<World> location, Optional<Float> price, Optional<Currency> currency, Optional<Inventory> itemsA, Optional<Inventory> itemsB) {

		String shopOwner = shopOwnerUUID != null ? shopOwnerUUID.toString() : "server";

		if (shopOwnerUUID != null) {
			Optional<Player> seller = Sponge.getServer().getPlayer(shopOwnerUUID);
			if (!seller.isPresent()) {
				ShopsData.soldSomethingOffline(shopOwnerUUID);
			}
		}

		JsonObject newNode = new JsonObject();
		newNode.addProperty("player", player.getName());
		newNode.addProperty("playerID", player.getUniqueId().toString());
		newNode.addProperty("time", System.currentTimeMillis());

		JsonObject locationNode = new JsonObject();
		locationNode.addProperty("world", location.getExtent().getName());
		locationNode.addProperty("worldID", location.getExtent().getUniqueId().toString());
		locationNode.addProperty("X", location.getBlockX());
		locationNode.addProperty("Y", location.getBlockY());
		locationNode.addProperty("Z", location.getBlockZ());

		Optional<TileEntity> sign = location.getTileEntity();
		if (sign.isPresent() && sign.get().supports(SignData.class)) {
			Optional<SignData> data = sign.get().getOrCreate(SignData.class);
			if (data.isPresent()) {
				locationNode.addProperty("line0", data.get().lines().get(0).toPlain());
				locationNode.addProperty("line1", data.get().lines().get(1).toPlain());
				locationNode.addProperty("line2", data.get().lines().get(2).toPlain());
				locationNode.addProperty("line3", data.get().lines().get(3).toPlain());
			}
		}

		newNode.add("sign", locationNode);

		if (ShopsData.hasMultipleCurrencies() && currency.isPresent()) {
			JsonObject currencyNode = new JsonObject();
			currencyNode.addProperty("currencySymbol", TextSerializers.FORMATTING_CODE.serialize(currency.get().getSymbol()));
			currencyNode.addProperty("currencySymbolplain", currency.get().getSymbol().toPlain());
			currencyNode.addProperty("currencyName", currency.get().getName());
			currencyNode.addProperty("currencyDName", TextSerializers.FORMATTING_CODE.serialize(currency.get().getDisplayName()));
			currencyNode.addProperty("currencyPDName", TextSerializers.FORMATTING_CODE.serialize(currency.get().getPluralDisplayName()));
			newNode.add("currency", currencyNode);
		}

		if (price.isPresent())
			newNode.addProperty("price", price.get());

		if (itemsA.isPresent())
			newNode.add("items", invToArray(itemsA.get()));

		if (itemsB.isPresent())
			newNode.add("items2", invToArray(itemsB.get()));

		try {
			File file = new File(carrotLogsFolder, shopOwner + "." + type);

			if (!file.exists()) {
				file.createNewFile();
				JsonObject firstNode = new JsonObject();
				firstNode.addProperty("init", true);
				firstNode.addProperty("time", System.currentTimeMillis());

				Files.write(file.toPath(), firstNode.toString().getBytes(), StandardOpenOption.APPEND);
			}
			Files.write(file.toPath(), ",".getBytes(), StandardOpenOption.APPEND);
			Files.write(file.toPath(), newNode.toString().getBytes(), StandardOpenOption.APPEND);

		} catch (IOException e) {
			CarrotShop.getLogger().error(Lang.CONSOLE_ERROR_LOGS.replace("%owner%", shopOwner).replace("%source%", player.getName()).replace("%error%", e.getMessage()));
		}
	}

	private static JsonArray invToArray(Inventory inv) {
		JsonArray array = new JsonArray();

		for (Inventory slot : inv.slots()) {
			if (slot.peek().isPresent()) {
				JsonObject item = new JsonObject();
				item.addProperty("id", slot.peek().get().getItem().getId());
				item.addProperty("name", Shop.getItemName(slot.peek().get()).toPlain());
				item.addProperty("quantity", slot.peek().get().getQuantity());
				array.add(item);
			}
		}
		return array;
	}

	public static void generateReport(CommandSource src, UUID target) {
		src.sendMessage(Text.of(TextColors.GOLD, Lang.REPORT_PREPARE));

		Task.builder().execute(() -> {
			Optional<String> query = ShopsLogs.getLog(target);

			if (!query.isPresent()) {
				src.sendMessage(Text.of(TextColors.DARK_RED, Lang.REPORT_ERROR_DATA));
				return;
			}

			String url = "http://carrotshop.xyz/create.php";
			String charset = java.nio.charset.StandardCharsets.UTF_8.name();

			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection) new URL(url).openConnection();

				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Accept-Charset", charset);
				connection.setRequestProperty("Content-Type", "application/json;charset=" + charset);


				OutputStream output = connection.getOutputStream();	
				output.write(query.get().getBytes(charset));

				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line+"\n");
				}
				br.close();

				JsonElement jelement = new JsonParser().parse(sb.toString());
				JsonObject  jobject = jelement.getAsJsonObject();

				if (jobject.get("status").getAsString().equals("error")) {
					src.sendMessage(Text.of(TextColors.DARK_RED, Lang.REPORT_ERROR_SERVER));
					CarrotShop.getLogger().error(Lang.CONSOLE_ERROR_GENERIC.replace("%error%", jobject.get("error").getAsString()));
					return ;
				}

				String reportURL = Lang.REPORT_URL.replace("%id%", jobject.get("id").getAsString());

				src.sendMessage(Text.of(TextColors.GOLD, Lang.split(Lang.REPORT_READY, "%url%", 0),
						Text.builder(reportURL)
						.color(TextColors.DARK_AQUA)
						.onClick(TextActions.openUrl(new URL(reportURL))).build(),
						TextColors.GOLD, Lang.split(Lang.REPORT_READY, "%url%", 1)));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				CarrotShop.getLogger().error(Lang.CONSOLE_ERROR_GENERIC.replace("%error%", e.getMessage()));
				if (connection != null)
					try {
						CarrotShop.getLogger().error(connection.getResponseMessage());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
			}

		}).async().name("CarrotShop - Report").submit(CarrotShop.getInstance());
	}

}
