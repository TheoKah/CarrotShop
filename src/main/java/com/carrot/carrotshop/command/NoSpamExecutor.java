package com.carrot.carrotshop.command;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import com.carrot.carrotshop.CarrotShop;

public class NoSpamExecutor implements CommandExecutor{

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if(!(src instanceof Player)) {
			src.sendMessage(Text.of(TextColors.DARK_RED, "Need to be a player"));
			return CommandResult.success();
		}

		if (CarrotShop.toggleSpam(((Player) src).getUniqueId()))
			src.sendMessage(Text.of(TextColors.DARK_GREEN, "Shop use report: OFF"));
		else
			src.sendMessage(Text.of(TextColors.DARK_GREEN, "Shop use report: ON"));

		return CommandResult.success();
	}

}
