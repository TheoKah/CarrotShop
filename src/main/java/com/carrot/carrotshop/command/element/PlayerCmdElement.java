package com.carrot.carrotshop.command.element;

import java.util.stream.Collectors;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.PatternMatchingCommandElement;
import org.spongepowered.api.text.Text;

public class PlayerCmdElement extends PatternMatchingCommandElement
{
	public PlayerCmdElement(Text key)
	{
		super(key);
	}
	
	@Override
	protected Iterable<String> getChoices(CommandSource src)
	{
		return Sponge.getServer().getGameProfileManager().getCache().getProfiles().stream().filter(gp -> gp.getName().isPresent()).map(gp -> gp.getName().get()).collect(Collectors.toList());
	}

	@Override
	protected Object getValue(String choice) throws IllegalArgumentException
	{
		return choice;
	}

	public Text getUsage(CommandSource src)
	{
		return Text.EMPTY;
	}
}
