package com.carrot.carrotshop.command;

import java.util.Optional;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;

import com.carrot.carrotshop.ShopsData;

public class ShopConfigCurrencyExecutor implements CommandExecutor{

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		Optional<Currency> cur = args.<Currency>getOne("currency");

		if (cur.isPresent()) {
			ShopsData.setCurrency(cur.get());
		}

		src.sendMessage(Text.of("Default currency is ", ShopsData.getCurrency().getDisplayName(),  " (" + ShopsData.getCurrency().getId() + ")"));
		
		return CommandResult.success();
	}

}
