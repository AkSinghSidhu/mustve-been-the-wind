package com.windsmoke;

import com.windsmoke.block.ModBlockEntities;
import com.windsmoke.block.ModBlocks;
import com.windsmoke.command.WindCommand;
import com.windsmoke.dev.TestHarness;
import com.windsmoke.item.ModItems;
import com.windsmoke.net.WindControlPayload;
import com.windsmoke.net.WindPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindSmoke implements ModInitializer {
	public static final String MOD_ID = "windsmoke";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		WindSmokeServerConfig.load();
		ModBlocks.register();
		ModBlockEntities.register();
		ModItems.register();
		PayloadTypeRegistry.clientboundPlay().register(WindPayload.TYPE, WindPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(WindControlPayload.TYPE, WindControlPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(WindControlPayload.TYPE, (payload, context) -> {
			var player = context.player();
			var server = context.server();
			// Integrated singleplayer is allowed even with cheats disabled. Once
			// the world is opened to LAN there is more than one player online, so
			// guests must hold the normal gamemaster permission instead.
			boolean allowed = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
				|| (server.isSingleplayer() && server.getPlayerList().getPlayerCount() == 1);
			if (!allowed) {
				player.sendSystemMessage(Component.translatable("windsmoke.debug.denied"));
				return;
			}

			if (!Float.isFinite(payload.angle()) || !Float.isFinite(payload.strength())) {
				player.sendSystemMessage(Component.translatable("windsmoke.debug.invalid"));
				return;
			}

			if (payload.release()) {
				WindManager.state().unpin();
				player.sendOverlayMessage(Component.translatable("windsmoke.debug.released"));
			} else {
				WindManager.state().set(payload.angle(), payload.strength());
				player.sendOverlayMessage(
					Component.translatable(
						"windsmoke.debug.applied",
						Math.round((payload.angle() % 360.0F + 360.0F) % 360.0F),
						Math.round(payload.strength() * 100.0F)
					)
				);
			}
			WindManager.syncAll(server);
		});
		CommandRegistrationCallback.EVENT.register(WindCommand::register);
		ServerTickEvents.END_SERVER_TICK.register(WindManager::tick);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> WindManager.reset());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> WindManager.reset());
		TestHarness.register();
	}
}
