package com.carrot.carrotshop.command;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import com.carrot.carrotshop.Lang;

public class ShopConfigExecutor implements CommandExecutor{

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		List<Text> contents = new ArrayList<>();

		contents.add(Text.of(TextColors.GOLD, "/cs config reload", TextColors.GRAY, " - ", TextColors.YELLOW, Lang.HELP_DESC_CMD_CONFIG_RELOAD));
		contents.add(Text.of(TextColors.GOLD, "/cs config currency [currency]", TextColors.GRAY, " - ", TextColors.YELLOW, Lang.HELP_DESC_CMD_CONFIG_CURRENCY));

		PaginationList.builder()
		.title(Text.of(TextColors.GOLD, "{ ", TextColors.YELLOW, Lang.HELP_HEADER_CMD_CONFIG, TextColors.GOLD, " }"))
		.contents(contents)
		.padding(Text.of("-"))
		.sendTo(src);
		return CommandResult.success();
	}

}
