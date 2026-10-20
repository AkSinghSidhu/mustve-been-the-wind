package com.windsmoke;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A transient wind event. The weather/time band gives the background wind;
 * an event briefly pushes the wind above it (gust, squall, gust front, breeze)
 * or below it (lull), then releases it. Durations are seconds; the runtime
 * envelope is shaped by {@link #attackFraction()} and {@link #releaseFraction()}.
 */
public enum WindEvent {
	BREEZE("breeze", "breeze", 8, 14, 0.08F, 0.16F, 0.0F, 0.30F, 0.45F, 35, 110),
	GUST("gust", "gust", 3, 5, 0.22F, 0.45F, 0.0F, 0.20F, 0.55F, 30, 100),
	LULL("lull", "lull", 3, 6, 0.35F, 0.65F, 0.0F, 0.20F, 0.55F, 30, 110),
	SQUALL("squall", "squall", 5, 9, 0.50F, 0.80F, 18.0F, 0.15F, 0.55F, 50, 160),
	GUST_FRONT("front", "gust front", 8, 14, 0.65F, 0.95F, 35.0F, 0.10F, 0.70F, 90, 240);

	private static final WindEvent[] CLEAR_WEATHER = {
		BREEZE, BREEZE, BREEZE,
		GUST, GUST, GUST, GUST,
		LULL, LULL, LULL,
		SQUALL,
		GUST_FRONT
	};
	private static final WindEvent[] RAIN_WEATHER = {
		BREEZE, BREEZE,
		GUST, GUST, GUST,
		LULL,
		SQUALL, SQUALL, SQUALL, SQUALL,
		GUST_FRONT, GUST_FRONT
	};
	private static final WindEvent[] THUNDER_WEATHER = {
		GUST, GUST,
		LULL,
		SQUALL, SQUALL, SQUALL,
		GUST_FRONT, GUST_FRONT, GUST_FRONT, GUST_FRONT
	};

	private final String id;
	private final String displayName;
	private final int minSeconds;
	private final int maxSeconds;
	private final float minPeak;
	private final float maxPeak;
	private final float veerDegrees;
	private final float attackFraction;
	private final float releaseFraction;
	private final int minCooldownSeconds;
	private final int maxCooldownSeconds;

	WindEvent(String id, String displayName, int minSeconds, int maxSeconds, float minPeak, float maxPeak,
			float veerDegrees, float attackFraction, float releaseFraction, int minCooldownSeconds,
			int maxCooldownSeconds) {
		this.id = id;
		this.displayName = displayName;
		this.minSeconds = minSeconds;
		this.maxSeconds = maxSeconds;
		this.minPeak = minPeak;
		this.maxPeak = maxPeak;
		this.veerDegrees = veerDegrees;
		this.attackFraction = attackFraction;
		this.releaseFraction = releaseFraction;
		this.minCooldownSeconds = minCooldownSeconds;
		this.maxCooldownSeconds = maxCooldownSeconds;
	}

	/** Command id, e.g. {@code front} for a gust front. */
	public String id() {
		return this.id;
	}

	/** Human-readable name used in the /wind report. */
	public String displayName() {
		return this.displayName;
	}

	public float attackFraction() {
		return this.attackFraction;
	}

	public float releaseFraction() {
		return this.releaseFraction;
	}

	public int durationTicks(RandomSource random) {
		int seconds = this.minSeconds + random.nextInt(this.maxSeconds - this.minSeconds + 1);
		return seconds * 20;
	}

	public float peak(RandomSource random) {
		return Mth.lerp(random.nextFloat(), this.minPeak, this.maxPeak);
	}

	/** Signed direction shift during the event; squalls and fronts veer. */
	public float veer(RandomSource random) {
		if (this.veerDegrees <= 0.0F) {
			return 0.0F;
		}

		float magnitude = this.veerDegrees * (0.7F + 0.3F * random.nextFloat());
		return random.nextBoolean() ? magnitude : -magnitude;
	}

	public int cooldownTicks(RandomSource random) {
		int seconds = this.minCooldownSeconds
			+ random.nextInt(this.maxCooldownSeconds - this.minCooldownSeconds + 1);
		return seconds * 20;
	}

	/** Weather-weighted random pick for scheduled events. */
	public static WindEvent pick(RandomSource random, boolean raining, boolean thundering) {
		WindEvent[] pool = thundering ? THUNDER_WEATHER : raining ? RAIN_WEATHER : CLEAR_WEATHER;
		return pool[random.nextInt(pool.length)];
	}

	public static WindEvent byId(String id) {
		String normalized = id.toLowerCase(java.util.Locale.ROOT).replace(' ', '_');

		if (normalized.equals("gust_front")) {
			normalized = "front";
		}

		for (WindEvent event : values()) {
			if (event.id.equals(normalized)) {
				return event;
			}
		}

		return null;
	}
}
