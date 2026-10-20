package com.windsmoke;

import com.windsmoke.command.WindCommand;
import com.windsmoke.dev.TestHarness;
import com.windsmoke.item.ModItems;
import com.windsmoke.net.WindPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindSmoke implements ModInitializer {
	public static final String MOD_ID = "windsmoke";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.register();
		PayloadTypeRegistry.playS2C().register(WindPayload.TYPE, WindPayload.CODEC);
		CommandRegistrationCallback.EVENT.register(WindCommand::register);
		ServerTickEvents.END_SERVER_TICK.register(WindManager::tick);
		TestHarness.register();
	}
}
