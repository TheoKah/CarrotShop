package com.carrot.carrotshop.command.importObjects;

import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class AdminShop
{
	@Setting
	public Location<World> signLocation;

	@Setting
	public ItemStackSnapshot item;

	@Setting
	public double price;

	@Setting
	public boolean buyShop;

	public AdminShop() {
	}
}