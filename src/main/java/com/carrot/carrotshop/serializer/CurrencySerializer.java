package com.carrot.carrotshop.serializer;

import org.spongepowered.api.service.economy.Currency;

import com.carrot.carrotshop.CarrotShop;
import com.carrot.carrotshop.ShopsData;
import com.google.common.reflect.TypeToken;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class CurrencySerializer implements TypeSerializer<Currency> {

	@Override
	public Currency deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
		String val = value.getNode("currency").getString();
		if (!ShopsData.hasMultipleCurrencies() || val == null || val.isEmpty())
			return null;
		for (Currency cur : CarrotShop.getEcoService().getCurrencies()) {
			if (val.equals(cur.getId()))
				return cur;
		}
		return null;
	}

	@Override
	public void serialize(TypeToken<?> type, Currency obj, ConfigurationNode value) throws ObjectMappingException {
		if (ShopsData.hasMultipleCurrencies() && obj != null)
			value.getNode("currency").setValue(obj.getId());
	}
}