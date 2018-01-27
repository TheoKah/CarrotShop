package com.carrot.carrotshop.command;

import java.net.MalformedURLException;
import java.net.URL;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import com.carrot.carrotshop.Lang;

public class ShopWikiExecutor implements CommandExecutor{

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		String wikiURL = "https://github.com/TheoKah/CarrotShop/wiki/User-Guide";

		try {
			src.sendMessage(Text.of(TextColors.DARK_PURPLE, Lang.split(Lang.WIKI_LINK, "%url%", 0),
					Text.builder(wikiURL)
					.color(TextColors.DARK_AQUA)
					.onClick(TextActions.openUrl(new URL(wikiURL))).build(),
					TextColors.DARK_PURPLE, Lang.split(Lang.WIKI_LINK, "%url%", 1)));
		} catch (MalformedURLException e) {
			src.sendMessage(Text.of(TextColors.DARK_PURPLE, Lang.split(Lang.WIKI_LINK, "%url%", 0),
					TextColors.DARK_AQUA, wikiURL,
					TextColors.DARK_PURPLE, Lang.split(Lang.WIKI_LINK, "%url%", 1)));
		}
		return CommandResult.success();
	}

}
