package com.carrot.carrotshop.command;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import com.carrot.carrotshop.Lang;
import com.carrot.carrotshop.ShopsLogs;

public class ShopReportExecutor implements CommandExecutor{

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		Optional<User> user = args.<User>getOne("player");
		
		UUID target = null;

		if (user.isPresent()) {
			if (!src.hasPermission("carrotshop.report.other")) {
				src.sendMessage(Text.of(TextColors.DARK_RED, Lang.REPORT_ERROR_OPERM));
				return CommandResult.success();
			}

			target = user.get().getUniqueId();

		} else {
			if (!src.hasPermission("carrotshop.report.self")) {
				src.sendMessage(Text.of(TextColors.DARK_RED, Lang.REPORT_ERROR_PERM));
				return CommandResult.success();
			}
			
			if(src instanceof Player) {
				target = ((Player) src).getUniqueId();
			}
			
		}

		ShopsLogs.generateReport(src, target);

		return CommandResult.success();
	}

}
