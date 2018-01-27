package com.carrot.carrotshop.listener;

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import com.carrot.carrotshop.Lang;
import com.carrot.carrotshop.ShopsData;

public class PlayerConnexionListener {

	@Listener(order=Order.LATE)
	public void onPlayerJoin(ClientConnectionEvent.Join event)
	{
		if (ShopsData.hasSoldSomethingOffline(event.getTargetEntity().getUniqueId()) && event.getTargetEntity().hasPermission("carrotshop.report.self")) {
			event.getTargetEntity().sendMessage(Text.of(TextColors.YELLOW, Lang.SHOP_USED.split("%cmd%")[0],
					Text.builder("/carrotreport")
					.color(TextColors.DARK_AQUA)
					.onClick(TextActions.runCommand("/carrotreport")).build(),
					TextColors.YELLOW, Lang.SHOP_USED.split("%cmd%")[1]));
		}
	}
}
