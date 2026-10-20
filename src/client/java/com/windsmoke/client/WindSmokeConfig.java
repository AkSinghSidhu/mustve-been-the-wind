package com.windsmoke.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.windsmoke.WindSmoke;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

/**
 * Client-side visual settings for the wind effects.
 *
 * <p>These options deliberately affect presentation only. The server remains
 * authoritative for the actual wind, so changing intensity or effect distance
 * never changes gameplay or the value shown by the Wind Vane.
 */
public final class WindSmokeConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance()
		.getConfigDir()
		.resolve("windsmoke.json");
	private static final float MIN_INTENSITY = 0.0F;
	private static final float MAX_INTENSITY = 2.0F;
	private static final int MIN_DISTANCE_CHUNKS = 4;
	private static final int MAX_DISTANCE_CHUNKS = 16;
	private static final int MIN_BANNER_SIMULATIONS = 0;
	private static final int MAX_BANNER_SIMULATIONS = 48;

	private static float windIntensity = 1.0F;
	private static int simulationDistanceChunks = 8;
	private static int visualQuality = 2;
	private static float smokeDensity = 1.0F;
	private static int maxBannerSimulations = 12;
	private static boolean loaded;

	private WindSmokeConfig() {
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
			if (data == null) {
				return;
			}
			windIntensity = clampIntensity(data.windIntensity);
			simulationDistanceChunks = clampDistance(data.simulationDistanceChunks);
			visualQuality = Mth.clamp(data.visualQuality, 0, 2);
			smokeDensity = Mth.clamp(data.smokeDensity, 0.0F, 1.0F);
			maxBannerSimulations = Mth.clamp(
				data.maxBannerSimulations,
				MIN_BANNER_SIMULATIONS,
				MAX_BANNER_SIMULATIONS
			);
		} catch (IOException | JsonParseException exception) {
			WindSmoke.LOGGER.warn("Could not read {}", FILE, exception);
		}
	}

	public static void save() {
		Data data = new Data();
		data.windIntensity = windIntensity;
		data.simulationDistanceChunks = simulationDistanceChunks;
		data.visualQuality = visualQuality;
		data.smokeDensity = smokeDensity;
		data.maxBannerSimulations = maxBannerSimulations;

		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(data), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			WindSmoke.LOGGER.warn("Could not write {}", FILE, exception);
		}
	}

	public static float intensity() {
		ensureLoaded();
		return windIntensity;
	}

	public static void setIntensity(float value) {
		ensureLoaded();
		windIntensity = clampIntensity(value);
		save();
	}

	public static int simulationDistanceChunks() {
		ensureLoaded();
		return simulationDistanceChunks;
	}

	public static void setSimulationDistanceChunks(int value) {
		ensureLoaded();
		simulationDistanceChunks = clampDistance(value);
		save();
	}

	public static int visualQuality() {
		ensureLoaded();
		return visualQuality;
	}

	public static void setVisualQuality(int value) {
		ensureLoaded();
		visualQuality = Mth.clamp(value, 0, 2);
		save();
	}

	public static float smokeDensity() {
		ensureLoaded();
		return smokeDensity;
	}

	public static void setSmokeDensity(float value) {
		ensureLoaded();
		smokeDensity = Mth.clamp(value, 0.0F, 1.0F);
		save();
	}

	public static int maxBannerSimulations() {
		ensureLoaded();
		return maxBannerSimulations;
	}

	public static void setMaxBannerSimulations(int value) {
		ensureLoaded();
		maxBannerSimulations = Mth.clamp(value, MIN_BANNER_SIMULATIONS, MAX_BANNER_SIMULATIONS);
		save();
	}

	public static void reset() {
		windIntensity = 1.0F;
		simulationDistanceChunks = 8;
		visualQuality = 2;
		smokeDensity = 1.0F;
		maxBannerSimulations = 12;
		save();
	}

	/**
	 * The server's current wind, multiplied by the player's visual preference.
	 * The shader and cloth simulation accept a little over 100% so a player can
	 * make calm weather readable without changing the server state.
	 */
	public static float visualStrength() {
		ensureLoaded();
		return Mth.clamp(WindClientState.strength() * windIntensity, 0.0F, 1.5F);
	}

	public static float effectDistanceBlocks() {
		ensureLoaded();
		return simulationDistanceChunks * 16.0F;
	}

	public static float minIntensity() {
		return MIN_INTENSITY;
	}

	public static float maxIntensity() {
		return MAX_INTENSITY;
	}

	public static int minDistanceChunks() {
		return MIN_DISTANCE_CHUNKS;
	}

	public static int maxDistanceChunks() {
		return MAX_DISTANCE_CHUNKS;
	}

	private static float clampIntensity(float value) {
		return Mth.clamp(value, MIN_INTENSITY, MAX_INTENSITY);
	}

	private static int clampDistance(int value) {
		return Mth.clamp(value, MIN_DISTANCE_CHUNKS, MAX_DISTANCE_CHUNKS);
	}

	private static void ensureLoaded() {
		if (!loaded) {
			load();
		}
	}

	private static final class Data {
		private float windIntensity = 1.0F;
		private int simulationDistanceChunks = 8;
		private int visualQuality = 2;
		private float smokeDensity = 1.0F;
		private int maxBannerSimulations = 12;
	}
}
