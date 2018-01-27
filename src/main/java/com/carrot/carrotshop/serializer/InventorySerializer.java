package com.carrot.carrotshop.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.InvalidDataFormatException;
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
		ConfigurationNode items = value.getNode("nbtitems");
		for (ConfigurationNode item : items.getChildrenList()) {
			Optional<ItemStack> itemStack;
			try {
				itemStack = ItemStack.builder().build(DataFormats.NBT.readFrom(new ByteArrayInputStream(Base64.getDecoder().decode(item.getString()))));
				if (itemStack.isPresent()) {
					inv.offer(itemStack.get());
				}
			} catch (InvalidDataException | InvalidDataFormatException | IOException e) {
				CarrotShop.getLogger().warn("Error occuered when loading item from NBT");
				e.printStackTrace();
			}

		}
		items = value.getNode("items");
		for (ConfigurationNode item : items.getChildrenList()) {
			inv.offer(item.getValue(TypeToken.of(ItemStack.class)));
		}
		return inv;
	}

	@Override
	public void serialize(TypeToken<?> type, Inventory obj, ConfigurationNode value) throws ObjectMappingException {
		ConfigurationNode items = value.getNode("nbtitems");
		ConfigurationNode failover = value.getNode("items");
		for (Inventory slot : obj.slots()) {
			if (slot.peek().isPresent()) {
				ConfigurationNode item = items.getAppendedNode();
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				try {
					DataFormats.NBT.writeTo(os, slot.peek().get().toContainer());
					os.flush();
					item.setValue(Base64.getEncoder().encodeToString(os.toByteArray()));
				} catch (IOException e) {
					ConfigurationNode failoveritem = failover.getAppendedNode();
					failoveritem.setValue(TypeToken.of(ItemStack.class), slot.peek().get());
					CarrotShop.getLogger().warn("Error occuered when saving item as NBT. Trying to save plain");
					e.printStackTrace();
				}
			}
		}
	}
}