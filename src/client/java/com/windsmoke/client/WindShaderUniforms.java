package com.windsmoke.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20;

/**
 * Uploads the live wind to the patched terrain shader whenever its program is
 * bound. Terrain shaders are compiled as several programs (solid, cutout,
 * translucent), so values are cached per program.
 */
public final class WindShaderUniforms {
	private static final Int2ObjectOpenHashMap<int[]> LOCATIONS = new Int2ObjectOpenHashMap<>();
	private static final float[] INFO = new float[4];
	private static final float[] WIND = new float[4];
	private static final float[] CAMERA = new float[4];
	private static final long START = System.nanoTime();
	private static volatile boolean changed = true;

	private WindShaderUniforms() {
	}

	public static void shadersChanged() {
		changed = true;
	}

	public static void onUse(int program) {
		if (program == 0) {
			return;
		}

		if (changed) {
			LOCATIONS.clear();
			changed = false;
		}

		int[] locations = LOCATIONS.get(program);

		if (locations == null) {
			locations = new int[]{
				GL20.glGetUniformLocation(program, "WindSmokeInfo"),
				GL20.glGetUniformLocation(program, "WindSmokeWind"),
				GL20.glGetUniformLocation(program, "WindSmokeCamera")
			};
			LOCATIONS.put(program, locations);
		}

		if (locations[0] < 0 && locations[1] < 0 && locations[2] < 0) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		boolean inWorld = minecraft.level != null && minecraft.gameRenderer != null;
		double seconds = (System.nanoTime() - START) / 1.0E9;

		INFO[0] = (float) (seconds % 3600.0);
		INFO[1] = inWorld ? WindSmokeConfig.visualStrength() : 0.0F;
		INFO[2] = 4321.5F;
		INFO[3] = inWorld ? WindSmokeConfig.visualQuality() : 0.0F;

		WIND[0] = (float) WindVisuals.windX();
		WIND[1] = (float) WindVisuals.windZ();
		WIND[2] = 0.0F;
		WIND[3] = 0.0F;

		if (inWorld) {
			var camera = minecraft.gameRenderer.mainCamera().position();
			CAMERA[0] = (float)camera.x;
			CAMERA[1] = (float)camera.y;
			CAMERA[2] = (float)camera.z;
		} else {
			CAMERA[0] = 0.0F;
			CAMERA[1] = 0.0F;
			CAMERA[2] = 0.0F;
		}

		CAMERA[3] = inWorld ? WindSmokeConfig.effectDistanceBlocks() : 0.0F;

		if (locations[0] >= 0) {
			GL20.glUniform4f(locations[0], INFO[0], INFO[1], INFO[2], INFO[3]);
		}

		if (locations[1] >= 0) {
			GL20.glUniform4f(locations[1], WIND[0], WIND[1], WIND[2], WIND[3]);
		}

		if (locations[2] >= 0) {
			GL20.glUniform4f(locations[2], CAMERA[0], CAMERA[1], CAMERA[2], CAMERA[3]);
		}
	}
}
