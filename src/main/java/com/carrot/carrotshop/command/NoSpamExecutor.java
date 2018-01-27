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
import com.carrot.carrotshop.Lang;

public class NoSpamExecutor implements CommandExecutor{

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if(!(src instanceof Player)) {
			src.sendMessage(Text.of(TextColors.DARK_RED, Lang.CONSOLE_ERROR_PLAYER));
			return CommandResult.success();
		}

		src.sendMessage(Text.of(TextColors.DARK_GREEN, Lang.CMD_SPAM.replace("%status%", CarrotShop.toggleSpam(((Player) src).getUniqueId()) ? Lang.STATUS_OFF : Lang.STATUS_ON)));
		return CommandResult.success();
	}

}
