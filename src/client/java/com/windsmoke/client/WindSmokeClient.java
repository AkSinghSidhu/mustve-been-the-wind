package com.windsmoke.client;

import com.windsmoke.net.WindPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class WindSmokeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(WindPayload.TYPE, (payload, context) ->
			WindClientState.set(payload.angle(), payload.strength()));
		ClientTickEvents.END_CLIENT_TICK.register(client -> WindVisuals.tick());
	}
}
