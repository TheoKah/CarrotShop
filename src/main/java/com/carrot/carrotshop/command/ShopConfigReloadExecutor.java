package com.carrot.carrotshop.command;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;

import com.carrot.carrotshop.Lang;
import com.carrot.carrotshop.ShopConfig;

public class ShopConfigReloadExecutor implements CommandExecutor{

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

		Lang.reload();
		ShopConfig.load();
		
		src.sendMessage(Text.of(Lang.CMD_CONFIG_RELOAD));
		
		return CommandResult.success();
	}

}
