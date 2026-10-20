package com.windsmoke;

import com.windsmoke.net.WindPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class WindManager {
	private static final WindState STATE = new WindState();

	private WindManager() {
	}

	public static WindState state() {
		return STATE;
	}

	/** Reset all server-authoritative wind state between worlds. */
	public static void reset() {
		STATE.reset();
	}

	public static void tick(MinecraftServer server) {
		STATE.tick(server);
		long time = server.overworld().getGameTime();

		// Gusts last only a few seconds, so push the wind to clients four times
		// a second; smoke, clouds and rain then follow the same envelope.
		if (time % 5L == 0L) {
			syncAll(server);
		}

		if (time % 20L == 0L) {
			SmokeAttraction.tick(server, STATE);
		}
	}

	/** Push the current wind to every player (4x a second, and on /wind set). */
	public static void syncAll(MinecraftServer server) {
		WindPayload payload = new WindPayload(STATE.angle(), STATE.strength());

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
