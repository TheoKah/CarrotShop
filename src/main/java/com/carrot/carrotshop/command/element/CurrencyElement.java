package com.carrot.carrotshop.command.element;

import java.util.ArrayList;
import java.util.List;


import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.PatternMatchingCommandElement;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;

import com.carrot.carrotshop.CarrotShop;

public class CurrencyElement extends PatternMatchingCommandElement
{
	public CurrencyElement(Text key)
	{
		super(key);
	}

	@Override
	protected Iterable<String> getChoices(CommandSource src)
	{
		List<String> items = new ArrayList<>();
		for (Currency cur : CarrotShop.getEcoService().getCurrencies()) {
			items.add(cur.getId());
			items.add(cur.getName());
		}
		return items;
	}

	@Override
	protected Object getValue(String choice) throws IllegalArgumentException
	{
		for (Currency cur : CarrotShop.getEcoService().getCurrencies()) {
			if (choice.equals(cur.getId()) || choice.equals(cur.getName())) {
				return cur;
			}
		}
		return CarrotShop.getEcoService().getDefaultCurrency();
	}

	public Text getUsage(CommandSource src)
	{
		return Text.EMPTY;
	}
}
