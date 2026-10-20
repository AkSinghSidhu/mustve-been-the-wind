package com.windsmoke;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The live wind for the overworld: a direction (degrees, 0 = blowing north,
 * 90 = blowing east) and a strength from 0 to 1.
 *
 * <p>Strength is driven by the weather: a calm clear day is barely a breeze
 * (~8%), steady rain is a strong wind (~50%) and a thunderstorm is a gale
 * (~100%). The wind eases toward that target instead of snapping, and the
 * direction meanders a little every ten seconds - further and faster when the
 * wind is strong.
 */
public final class WindState {
	private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	/** Wind strength on a clear, calm day. */
	private static final float CALM_STRENGTH = 0.08F;
	/** Added strength at full rain. */
	private static final float RAIN_STRENGTH = 0.42F;
	/** Added strength at full thunder (on top of the rain bonus). */
	private static final float THUNDER_STRENGTH = 0.50F;
	/** Per-tick ease toward the weather target (~0.16/s). */
	private static final float STRENGTH_EASE = 0.008F;

	private float angle = 90.0F;
	private float strength = CALM_STRENGTH;
	private float targetAngle = 90.0F;
	private float targetStrength = CALM_STRENGTH;
	private boolean pinned;
	private long nextRetarget;

	public void tick(MinecraftServer server) {
		if (this.pinned) {
			return;
		}

		ServerLevel overworld = server.overworld();
		long time = overworld.getGameTime();
		float weatherStrength = weatherStrength(overworld);

		if (time >= this.nextRetarget) {
			this.nextRetarget = time + 200L;
			RandomSource random = overworld.getRandom();

			// Storms veer further and faster than a light breeze.
			float spread = 20.0F + 70.0F * this.strength;
			this.targetAngle = Mth.wrapDegrees(this.angle + (random.nextFloat() - 0.5F) * spread);
			this.targetStrength = Mth.clamp(weatherStrength + (random.nextFloat() - 0.5F) * 0.08F, 0.0F, 1.0F);
		}

		float diff = Mth.wrapDegrees(this.targetAngle - this.angle);
		this.angle = Mth.wrapDegrees(this.angle + Mth.clamp(diff * 0.01F, -1.0F, 1.0F));
		this.strength += (this.targetStrength - this.strength) * STRENGTH_EASE;
	}

	/**
	 * Target strength for the current weather. Rain and thunder levels are
	 * interpolated, so the wind ramps up and down as a storm rolls in or clears.
	 */
	public static float weatherStrength(ServerLevel level) {
		float rain = Mth.clamp(level.getRainLevel(1.0F), 0.0F, 1.0F);
		float thunder = Mth.clamp(level.getThunderLevel(1.0F), 0.0F, 1.0F);
		return Mth.clamp(CALM_STRENGTH + RAIN_STRENGTH * rain + THUNDER_STRENGTH * thunder, 0.0F, 1.0F);
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
		this.nextRetarget = 0L;
	}

	/** Release the wind back to its natural drift. */
	public void unpin() {
		this.pinned = false;
		this.nextRetarget = 0L;
	}

	public boolean pinned() {
		return this.pinned;
	}
}
