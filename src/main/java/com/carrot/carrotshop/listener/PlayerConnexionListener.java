package com.carrot.carrotshop.listener;

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import com.carrot.carrotshop.ShopsData;

public class PlayerConnexionListener {

	@Listener(order=Order.LATE)
	public void onPlayerJoin(ClientConnectionEvent.Join event)
	{
		if (ShopsData.hasSoldSomethingOffline(event.getTargetEntity().getUniqueId()) && event.getTargetEntity().hasPermission("carrotshop.report.self")) {
			event.getTargetEntity().sendMessage(Text.of(TextColors.YELLOW, "Someone used your shop signs while you were away. Use ",
					Text.builder("/cs report")
					.color(TextColors.DARK_AQUA)
					.onClick(TextActions.runCommand("/cs report")).build(),
					TextColors.YELLOW, " for more details" ));
		}
	}
}
