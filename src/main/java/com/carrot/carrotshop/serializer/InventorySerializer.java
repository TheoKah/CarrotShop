package com.carrot.carrotshop.serializer;

import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;

import com.carrot.carrotshop.CarrotShop;
import com.google.common.reflect.TypeToken;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class InventorySerializer implements TypeSerializer<Inventory> {

	@Override
	public Inventory deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
		Inventory inv = Inventory.builder().build(CarrotShop.getInstance());
		ConfigurationNode items = value.getNode("items");
		for (ConfigurationNode item : items.getChildrenList()) {
			inv.offer(item.getValue(TypeToken.of(ItemStack.class)));
		}
		return inv;
	}

	@Override
	public void serialize(TypeToken<?> type, Inventory obj, ConfigurationNode value) throws ObjectMappingException {
		ConfigurationNode items = value.getNode("items");
		for (Inventory slot : obj.slots()) {
			if (slot.peek().isPresent()) {
				ConfigurationNode item = items.getAppendedNode();
				item.setValue(TypeToken.of(ItemStack.class), slot.peek().get());
			}
		}
	}
}