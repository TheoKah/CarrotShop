package com.carrot.carrotshop.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import com.carrot.carrotshop.CarrotShop;
import com.carrot.carrotshop.ShopsLogs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ShopReportExecutor implements CommandExecutor{

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		Optional<String> targetName = args.<String>getOne("player");

		UUID target;

		if (targetName.isPresent()) {
			if (!src.hasPermission("carrotshop.report.other")) {
				src.sendMessage(Text.of(TextColors.DARK_RED, "You do not have permission to generate reports for other players"));
				return CommandResult.success();
			}

			Optional<Player> onlinePlayer = Sponge.getServer().getPlayer(targetName.get());

			if (onlinePlayer.isPresent()) {
				target = onlinePlayer.get().getUniqueId();
			} else {
				Optional<UserStorageService> userStorage = Sponge.getServiceManager().provide(UserStorageService.class);

				Optional<User> offlinePlayer = userStorage.get().get(targetName.get());
				if (!offlinePlayer.isPresent()) {
					src.sendMessage(Text.of(TextColors.DARK_RED, "Player unknown"));
					return CommandResult.success();
				}
				target = offlinePlayer.get().getUniqueId();
			}

		} else {
			if(!(src instanceof Player)) {
				src.sendMessage(Text.of(TextColors.DARK_RED, "You need to specify a player name"));
				return CommandResult.success();
			}
			if (!src.hasPermission("carrotshop.report.self")) {
				src.sendMessage(Text.of(TextColors.DARK_RED, "You do not have permission to generate reports"));
				return CommandResult.success();
			}
			target = ((Player) src).getUniqueId();
		}

		src.sendMessage(Text.of(TextColors.GOLD, "The report is being prepared..."));

		Optional<String> query = ShopsLogs.getLog(target);
		
		if (!query.isPresent()) {
			src.sendMessage(Text.of(TextColors.DARK_RED, "No data found to generate report"));
			return CommandResult.success();
		}
		
		Task.builder().execute(() -> {
			String url = "https://carrotshop-ffb97.firebaseio.com/shop.json";
			String charset = java.nio.charset.StandardCharsets.UTF_8.name();

			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection) new URL(url).openConnection();

				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Accept-Charset", charset);
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);


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
	            
				
				String reportURL = "http://carrotshop.xyz/" + jobject.get("name").getAsString() + ".htm";

				src.sendMessage(Text.of(TextColors.GOLD, "Report is ready: ", Text.builder(reportURL)
						.color(TextColors.DARK_AQUA)
						.onClick(TextActions.openUrl(new URL(reportURL))).build()));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				CarrotShop.getLogger().error("ERROR: " + e.getMessage());
				if (connection != null)
					try {
						CarrotShop.getLogger().error(connection.getResponseMessage());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
			}
			
		}).async().name("CarrotShop - Report").submit(CarrotShop.getInstance());

		return CommandResult.success();
	}

}
