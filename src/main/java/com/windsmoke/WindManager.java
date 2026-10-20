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

	public static void tick(MinecraftServer server) {
		STATE.tick(server);

		if (server.overworld().getGameTime() % 20L == 0L) {
			syncAll(server);
			SmokeAttraction.tick(server, STATE);
		}
	}

	/** Push the current wind to every player (once a second, and on /wind set). */
	public static void syncAll(MinecraftServer server) {
		WindPayload payload = new WindPayload(STATE.angle(), STATE.strength());

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
