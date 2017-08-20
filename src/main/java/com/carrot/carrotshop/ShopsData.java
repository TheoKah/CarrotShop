package com.carrot.carrotshop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.carrot.carrotshop.serializer.InventorySerializer;
import com.carrot.carrotshop.shop.Bank;
import com.carrot.carrotshop.shop.Buy;
import com.carrot.carrotshop.shop.Heal;
import com.carrot.carrotshop.shop.Sell;
import com.carrot.carrotshop.shop.Shop;
import com.carrot.carrotshop.shop.Trade;
import com.carrot.carrotshop.shop.iBuy;
import com.carrot.carrotshop.shop.iSell;
import com.carrot.carrotshop.shop.iTrade;
import com.google.common.reflect.TypeToken;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;


public class ShopsData {
	private static File carrotshopsFile;
	private static ConfigurationNode shopsNode;
	private static ConfigurationLoader<CommentedConfigurationNode> loader;
	private static Map<Location<World>, Shop> shops = new HashMap<>();
	private static Hashtable<UUID, Stack<Location<World>>> storedLocations = new Hashtable<>();

	public static void init(File rootDir) throws IOException
	{
		carrotshopsFile = new File(rootDir, "shops.json");
		rootDir.mkdirs();
		carrotshopsFile.createNewFile();

		TypeSerializerCollection serializers = TypeSerializers.getDefaultSerializers().newChild();
		serializers.registerType(TypeToken.of(Inventory.class), new InventorySerializer());
		ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(serializers);

		loader = HoconConfigurationLoader.builder().setFile(carrotshopsFile).build();
		shopsNode = loader.load(options);
	}

	public static void load() {
		boolean hasErrors = false;

		for (ConfigurationNode shopNode : shopsNode.getNode("shops").getChildrenList()) {
			try {
				Shop shop = (Shop) shopNode.getNode("shop").getValue(TypeToken.of(Class.forName(shopNode.getNode("type").getString())));
				for (Location<World> location : shop.getLocations())
					shops.put(location, shop);
				shop.update();
			} catch (Exception e) {
				e.printStackTrace();
				hasErrors = true;
			}
		}
		if (hasErrors)
			CarrotShop.getLogger().warn("Errors occured while loading CarrotShops.");
	}

	public static void unload() {
		save();
		storedLocations.clear();
		shops.forEach((location,shop) -> {
			shop.setReset();
		});
		shops.clear();
	}

	public static void save() {
		boolean hasErrors = false;
		shopsNode.removeChild("shops");
		for (Entry<Location<World>, Shop> entry : shops.entrySet()) {
			if (entry.getKey().equals(entry.getValue().getLocations().get(0))) {
				ConfigurationNode shopNode = shopsNode.getNode("shops").getAppendedNode();
				Shop shop = entry.getValue();
				try {
					if (shop instanceof iTrade)
						shopNode.getNode("shop").setValue(TypeToken.of(iTrade.class), (iTrade) shop);
					else if (shop instanceof iBuy)
						shopNode.getNode("shop").setValue(TypeToken.of(iBuy.class), (iBuy) shop);
					else if (shop instanceof iSell)
						shopNode.getNode("shop").setValue(TypeToken.of(iSell.class), (iSell) shop);
					else if (shop instanceof Trade)
						shopNode.getNode("shop").setValue(TypeToken.of(Trade.class), (Trade) shop);
					else if (shop instanceof Buy)
						shopNode.getNode("shop").setValue(TypeToken.of(Buy.class), (Buy) shop);
					else if (shop instanceof Sell)
						shopNode.getNode("shop").setValue(TypeToken.of(Sell.class), (Sell) shop);
					else if (shop instanceof Bank)
						shopNode.getNode("shop").setValue(TypeToken.of(Bank.class), (Bank) shop);
					else if (shop instanceof Heal)
						shopNode.getNode("shop").setValue(TypeToken.of(Heal.class), (Heal) shop);
					else
						continue;
					shopNode.getNode("type").setValue(entry.getValue().getClass().getName());
				} catch (ObjectMappingException e) {
					e.printStackTrace();
					hasErrors = true;
				}
			}
		}
		try {
			loader.save(shopsNode);
		} catch (Exception e) {
			e.printStackTrace();
			hasErrors = true;
		}
		if (hasErrors)
			CarrotShop.getLogger().error("Unable to save all CarrotShops");
	}

	public static void addShop(Shop shop) {
		for (Location<World> location : shop.getLocations()) {
			shops.put(location, shop);
		}
		shop.update();
		save();
	}	

	public static void delShop(Shop shop) {
		shop.setReset();
		for (Location<World> location : shop.getLocations())
			shops.remove(location);
		save();
	}

	public static Optional<Shop> getShop(Location<World> loc) {
		if (shops.containsKey(loc))
			return Optional.of(shops.get(loc));
		return Optional.empty();
	}

	public static void storeItemLocation(Player player, Location<World> loc) {
		Stack<Location<World>> items = storedLocations.getOrDefault(player.getUniqueId(), new Stack<>());
		if (items.contains(loc)) {
			items.remove(loc);
			player.sendMessage(Text.of("Removed location of chest"));
			return ;
		} else {
			items.push(loc);
			player.sendMessage(Text.of("Stored location of chest"));
		}
		storedLocations.put(player.getUniqueId(), items);
	}

	public static Stack<Location<World>> getItemLocations(Player player) {
		return storedLocations.getOrDefault(player.getUniqueId(), new Stack<>());
	}

	public static void clearItemLocations(Player player) {
		storedLocations.remove(player.getUniqueId());

	}

}
