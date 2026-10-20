package com.windsmoke;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The live wind for the overworld: a direction (degrees, 0 = blowing north,
 * 90 = blowing east) and a strength from 0 to 1.
 *
 * <p>The wind meanders toward a new target every ten seconds; weather feeds
 * the target strength (clear &lt; rain &lt; thunder).
 */
public final class WindState {
	private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	private float angle = 90.0F;
	private float strength = 0.3F;
	private float targetAngle = 90.0F;
	private float targetStrength = 0.3F;
	private long nextRetarget;

	public void tick(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		long time = overworld.getGameTime();

		if (time >= this.nextRetarget) {
			this.nextRetarget = time + 200L;
			RandomSource random = overworld.getRandom();
			this.targetAngle = Mth.wrapDegrees(this.angle + random.nextFloat() * 90.0F - 45.0F);
			float base = 0.15F + random.nextFloat() * 0.25F;
			if (overworld.isRaining()) {
				base += 0.2F;
			}
			if (overworld.isThundering()) {
				base += 0.25F;
			}
			this.targetStrength = Mth.clamp(base, 0.0F, 1.0F);
		}

		float diff = Mth.wrapDegrees(this.targetAngle - this.angle);
		this.angle = Mth.wrapDegrees(this.angle + Mth.clamp(diff * 0.01F, -1.0F, 1.0F));
		this.strength += (this.targetStrength - this.strength) * 0.01F;
	}

	public float angle() {
		return this.angle;
	}

	public float strength() {
		return this.strength;
	}

	/** Unit wind vector on the X axis (east positive). */
	public double windX() {
		return Mth.sin(this.angle * Mth.DEG_TO_RAD);
	}

	/** Unit wind vector on the Z axis (south positive, so north is -Z). */
	public double windZ() {
		return -Mth.cos(this.angle * Mth.DEG_TO_RAD);
	}

	public String compass() {
		return COMPASS[Math.floorMod(Math.round(this.angle / 45.0F), 8)];
	}

	/** Pin the wind (used by /wind set and tests). */
	public void set(float angle, float strength) {
		this.angle = Mth.wrapDegrees(angle);
		this.targetAngle = this.angle;
		this.strength = Mth.clamp(strength, 0.0F, 1.0F);
		this.targetStrength = this.strength;
	}
}
