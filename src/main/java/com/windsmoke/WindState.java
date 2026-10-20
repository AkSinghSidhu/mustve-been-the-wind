package com.windsmoke;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The live wind for the overworld: a direction (degrees, 0 = blowing north,
 * 90 = blowing east) and a strength from 0 to 1.
 *
 * <p>Strength is not a fixed weather value: every few seconds the wind picks a
 * new target inside the current {@link WindClimate.Range} (clear 2-8% by day,
 * rain 25-50%, thunderstorm 80-100%, with night and dawn/dusk variants) and
 * eases toward it. That makes a calm day breathe between light airs and a
 * fresh breeze instead of sitting on one number. Direction meanders separately
 * and swings further when the wind is strong.
 */
public final class WindState {
	private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	/** How fast the wind eases toward a new strength target, per tick. */
	private static final float STRENGTH_EASE = 0.03F;
	/** How fast the direction eases toward its target, per tick (degrees). */
	private static final float DIRECTION_EASE = 0.02F;

	private WindClimate.SeasonProfile season = WindClimate.SeasonProfile.TEMPERATE;
	private float angle = 90.0F;
	private float strength = 0.05F;
	private float targetAngle = 90.0F;
	private float targetStrength = 0.05F;
	private boolean pinned;
	private long nextDirection;
	private long nextStrength;

	public void tick(MinecraftServer server) {
		if (this.pinned) {
			return;
		}

		ServerLevel overworld = server.overworld();
		long time = overworld.getGameTime();
		WindClimate.Range range = WindClimate.range(overworld, this.season);

		// Direction wanders every ten seconds; a gale swings much further than
		// a light breeze.
		if (time >= this.nextDirection) {
			this.nextDirection = time + 200L;
			RandomSource random = overworld.getRandom();
			float spread = 15.0F + 70.0F * this.strength;
			this.targetAngle = Mth.wrapDegrees(this.angle + (random.nextFloat() - 0.5F) * spread);
		}

		// Strength picks a fresh gust inside the weather/time band every 3-6 s.
		if (time >= this.nextStrength) {
			this.nextStrength = time + 60L + overworld.getRandom().nextInt(60);
			this.targetStrength = range.sample(overworld.getRandom());
		}

		float turn = Mth.wrapDegrees(this.targetAngle - this.angle);
		this.angle = Mth.wrapDegrees(this.angle + Mth.clamp(turn * DIRECTION_EASE, -1.5F, 1.5F));
		this.strength = Mth.clamp(this.strength + (this.targetStrength - this.strength) * STRENGTH_EASE, 0.0F, 1.0F);
	}

	/** The band the wind is currently sampling from (weather + time of day). */
	public WindClimate.Range currentRange(MinecraftServer server) {
		return WindClimate.range(server.overworld(), this.season);
	}

	/** Hook for a future season system. */
	public void setSeasonProfile(WindClimate.SeasonProfile season) {
		this.season = season;
	}

	public String weatherLabel(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (level.isThundering()) {
			return "thunderstorm";
		}
		if (level.isRaining()) {
			return "rain";
		}
		return "clear";
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
		this.pinned = true;
		this.nextDirection = 0L;
		this.nextStrength = 0L;
	}

	/** Release the wind back to its natural drift. */
	public void unpin() {
		this.pinned = false;
		this.nextDirection = 0L;
		this.nextStrength = 0L;
	}

	public boolean pinned() {
		return this.pinned;
	}
}
