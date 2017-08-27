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

public class ShopMainExecutor implements CommandExecutor{

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		List<Text> contents = new ArrayList<>();

		contents.add(Text.of(TextColors.GOLD, "/cs help", TextColors.GRAY, " - ", TextColors.YELLOW, "Print a link to the wiki"));
		contents.add(Text.of(TextColors.GOLD, "/cs hide", TextColors.GRAY, " - ", TextColors.YELLOW, "Hide sign usage from chat"));
		contents.add(Text.of(TextColors.GOLD, "/cs report", TextColors.GRAY, " - ", TextColors.YELLOW, "Generate a personal shop report"));
		contents.add(Text.of(TextColors.GOLD, "/cs servreport", TextColors.GRAY, " - ", TextColors.YELLOW, "Generate a server shop report"));
		contents.add(Text.of(TextColors.GOLD, "/cs report [player]", TextColors.GRAY, " - ", TextColors.YELLOW, "Generate a shop report for another player"));


		PaginationList.builder()
		.title(Text.of(TextColors.GOLD, "{ ", TextColors.YELLOW, "/carrotshop", TextColors.GOLD, " }"))
		.contents(contents)
		.padding(Text.of("-"))
		.sendTo(src);
		return CommandResult.success();
	}

}
