package com.carrot.carrotshop.command;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import com.carrot.carrotshop.Lang;
import com.carrot.carrotshop.ShopsLogs;

public class ShopServerReportExecutor implements CommandExecutor{

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if (!src.hasPermission("carrotshop.report.server")) {
			src.sendMessage(Text.of(TextColors.DARK_RED, Lang.REPORT_ERROR_OPERM));
			return CommandResult.success();
		}		

		ShopsLogs.generateReport(src, null);

		return CommandResult.success();
	}

}
