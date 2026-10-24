package com.windsmoke.client;

import com.windsmoke.block.ModBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import com.mojang.blaze3d.platform.InputConstants;
import com.windsmoke.net.WindPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import org.lwjgl.glfw.GLFW;

public class WindSmokeClient implements ClientModInitializer {
	private static KeyMapping configKey;

	@Override
	public void onInitializeClient() {
		WindSmokeConfig.load();
		if (FabricLoader.getInstance().isModLoaded("sodium")) {
			com.windsmoke.WindSmoke.LOGGER.info(
				"Sodium detected: Sodium terrain wind sway integration enabled."
			);
		}
		BlockEntityRenderers.register(ModBlockEntities.WIND_VANE, context -> new WindVaneRenderer());
		configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.windsmoke.config",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_F8,
			KeyMapping.Category.MISC
		));

		ClientPlayNetworking.registerGlobalReceiver(WindPayload.TYPE, (payload, context) ->
			WindClientState.set(payload.angle(), payload.strength()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			WindClientState.reset();
			WindVisuals.reset();
			BannerClothSimulation.clear();
			BannerIdentity.clear();
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			WindVisuals.tick();
			while (configKey.consumeClick()) {
				client.setScreenAndShow(new WindConfigScreen(client.gui.screen()));
			}
		});
	}
}
