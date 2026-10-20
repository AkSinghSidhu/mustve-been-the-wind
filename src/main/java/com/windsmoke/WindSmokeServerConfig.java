package com.windsmoke;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

/**
 * Small server-side gameplay settings, persisted separately from the client
 * visual preferences. Smoke attraction is experimental, so it can be turned off
 * without removing the visual smoke or opening a client config screen.
 */
public final class WindSmokeServerConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance()
		.getConfigDir()
		.resolve("windsmoke-server.json");
	private static final int MIN_RADIUS = 8;
	private static final int MAX_RADIUS = 64;

	private static boolean smokeAttractsMobs = true;
	private static int smokeAttractionRadius = 24;
	private static boolean loaded;

	private WindSmokeServerConfig() {
	}

	public static void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		if (!Files.isRegularFile(FILE)) {
			return;
		}

		try {
			Data data = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), Data.class);
			if (data != null) {
				smokeAttractsMobs = data.smokeAttractsMobs;
				smokeAttractionRadius = Mth.clamp(
					data.smokeAttractionRadius, MIN_RADIUS, MAX_RADIUS);
			}
		} catch (IOException | JsonParseException exception) {
			WindSmoke.LOGGER.warn("Could not read {}", FILE, exception);
		}
	}

	public static void save() {
		Data data = new Data();
		data.smokeAttractsMobs = smokeAttractsMobs;
		data.smokeAttractionRadius = smokeAttractionRadius;
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(data), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			WindSmoke.LOGGER.warn("Could not write {}", FILE, exception);
		}
	}

	public static boolean smokeAttractsMobs() {
		ensureLoaded();
		return smokeAttractsMobs;
	}

	public static void setSmokeAttractsMobs(boolean value) {
		ensureLoaded();
		smokeAttractsMobs = value;
		save();
	}

	public static int smokeAttractionRadius() {
		ensureLoaded();
		return smokeAttractionRadius;
	}

	public static void setSmokeAttractionRadius(int value) {
		ensureLoaded();
		smokeAttractionRadius = Mth.clamp(value, MIN_RADIUS, MAX_RADIUS);
		save();
	}

	private static void ensureLoaded() {
		if (!loaded) {
			load();
		}
	}

	private static final class Data {
		private boolean smokeAttractsMobs = true;
		private int smokeAttractionRadius = 24;
	}
}
