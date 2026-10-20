package com.windsmoke;

import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The live wind for the overworld: a direction (degrees, 0 = blowing north,
 * 90 = blowing east) and a strength from 0 to 1.
 *
 * <p>The weather/time {@link WindClimate.Range} is the background wind. The
 * wind picks a new target inside that band every 3-6 s and eases toward it, and
 * a slow two-sine cycle biases those targets up and down over a few minutes so
 * a long thunderstorm is never a flat 90%.
 *
 * <p>On top of that, transient {@link WindEvent}s arrive on their own schedule:
 * gusts, lulls, breezes, squalls and gust fronts. Each event has a smooth
 * attack/hold/release envelope, lasts a few seconds (longer for a front), and
 * can veer the direction. A gust front is also triggered automatically at
 * thunderstorm onset.
 */
public final class WindState {
	private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
	private static final float STRENGTH_EASE = 0.03F;
	private static final float EVENT_STRENGTH_EASE = 0.07F;
	private static final float DIRECTION_EASE = 0.02F;
	private static final float EVENT_DIRECTION_EASE = 0.045F;

	private WindClimate.SeasonProfile season = WindClimate.SeasonProfile.TEMPERATE;
	private float angle = 90.0F;
	private float strength = 0.05F;
	private float targetAngle = 90.0F;
	private float targetStrength = 0.05F;
	private boolean pinned;
	private long nextDirection;
	private long nextStrength;

	private boolean scheduled;
	private long nextEvent;
	private WindEvent event;
	private long eventStart;
	private long eventEnd;
	private float eventPeak;
	private float eventVeer;
	private float previousRain;
	private float previousThunder;
	private boolean weatherSeen;

	public void tick(MinecraftServer server) {
		if (this.pinned) {
			return;
		}

		ServerLevel overworld = server.overworld();
		long time = overworld.getGameTime();
		RandomSource random = overworld.getRandom();
		float rain = Mth.clamp(overworld.getRainLevel(1.0F), 0.0F, 1.0F);
		float thunder = Mth.clamp(overworld.getThunderLevel(1.0F), 0.0F, 1.0F);

		this.updateWeather(time, random, rain, thunder);
		WindClimate.Range range = WindClimate.range(overworld, this.season);

		if (!this.scheduled) {
			this.scheduled = true;
			this.nextEvent = time + 200L + random.nextInt(400);
		}

		if (this.event != null && time >= this.eventEnd) {
			if (Boolean.getBoolean("windsmoke.testharness")) {
				WindSmoke.LOGGER.info("[windsmoke] event end {}", this.event.id());
			}

			this.event = null;
		}

		if (this.event == null && time >= this.nextEvent) {
			this.startEvent(WindEvent.pick(random, rain > 0.1F, thunder > 0.1F), time, random, "random", false);
		}

		// The base direction wanders every ten seconds; a gale swings further
		// than a light breeze. Anchor the walk to the target rather than the
		// live angle so an event veer does not permanently drag the base around.
		if (time >= this.nextDirection) {
			this.nextDirection = time + 200L;
			float spread = 15.0F + 70.0F * this.strength;
			this.targetAngle = Mth.wrapDegrees(this.targetAngle + (random.nextFloat() - 0.5F) * spread);
		}

		// Pick a fresh background target inside the weather/time band. The slow
		// cycle controls how much of the band is in play, so the wind drifts up
		// and down over minutes even when the weather itself is steady.
		if (time >= this.nextStrength) {
			this.nextStrength = time + 60L + random.nextInt(60);
			float coverage = 0.45F + 0.55F * slowWave(time);
			this.targetStrength = range.sample(random, coverage);
		}

		float envelope = this.eventEnvelope(time);
		float desiredStrength = this.targetStrength;
		float desiredAngle = this.targetAngle;

		if (this.event != null) {
			float impact = this.eventPeak * envelope;

			if (this.event == WindEvent.LULL) {
				desiredStrength = this.targetStrength * (1.0F - impact);
			} else {
				desiredStrength = this.targetStrength + (1.0F - this.targetStrength) * impact;
			}

			desiredAngle = Mth.wrapDegrees(this.targetAngle + this.eventVeer * envelope);
		}

		desiredStrength = Mth.clamp(desiredStrength, 0.0F, 1.0F);
		float strengthEase = this.event != null ? EVENT_STRENGTH_EASE : STRENGTH_EASE;
		this.strength = Mth.clamp(
			this.strength + (desiredStrength - this.strength) * strengthEase, 0.0F, 1.0F);

		float turn = Mth.wrapDegrees(desiredAngle - this.angle);
		float directionEase = this.event != null ? EVENT_DIRECTION_EASE : DIRECTION_EASE;
		this.angle = Mth.wrapDegrees(
			this.angle + Mth.clamp(turn * directionEase, -2.5F, 2.5F));
	}

	private void updateWeather(long time, RandomSource random, float rain, float thunder) {
		if (!this.weatherSeen) {
			this.weatherSeen = true;
			this.previousRain = rain;
			this.previousThunder = thunder;
			return;
		}

		if (this.previousThunder <= 0.1F && thunder > 0.1F) {
			// A thunderstorm arrives with a gust front. It replaces any weaker
			// event that was already running.
			this.startEvent(WindEvent.GUST_FRONT, time, random, "storm onset", true);
		} else if (this.previousRain <= 0.1F && rain > 0.1F && thunder <= 0.1F) {
			this.startEvent(WindEvent.SQUALL, time, random, "rain onset", false);
		}

		this.previousRain = rain;
		this.previousThunder = thunder;
	}

	private boolean startEvent(WindEvent type, long time, RandomSource random, String cause, boolean replace) {
		if (this.event != null && !replace) {
			return false;
		}

		this.event = type;
		this.eventStart = time;
		this.eventEnd = time + type.durationTicks(random);
		this.eventPeak = type.peak(random);
		this.eventVeer = type.veer(random);
		this.nextEvent = this.eventEnd + type.cooldownTicks(random);

		if (Boolean.getBoolean("windsmoke.testharness")) {
			WindSmoke.LOGGER.info(
				"[windsmoke] event start {} cause={} duration={}t peak={} veer={}",
				type.id(), cause, this.eventEnd - this.eventStart,
				String.format(Locale.ROOT, "%.2f", this.eventPeak),
				String.format(Locale.ROOT, "%.0f", this.eventVeer));
		}

		return true;
	}

	/** Smooth 0-1-0 envelope with an event-specific attack and release. */
	private float eventEnvelope(long time) {
		if (this.event == null) {
			return 0.0F;
		}

		long duration = this.eventEnd - this.eventStart;
		if (duration <= 0L) {
			return 0.0F;
		}

		float progress = Mth.clamp((time - this.eventStart) / (float)duration, 0.0F, 1.0F);
		float attack = this.event.attackFraction();
		float release = this.event.releaseFraction();

		if (progress < attack) {
			return smooth(progress / attack);
		}

		if (progress > 1.0F - release) {
			return smooth((1.0F - progress) / release);
		}

		return 1.0F;
	}

	/**
	 * Two out-of-phase sines with periods of roughly 2.5 and 6 minutes. The
	 * result (0-1) changes how much of the current band the background wind
	 * samples, producing slow rises and falls that are not tied to gusts.
	 */
	private static float slowWave(long time) {
		float fast = Mth.sin(time * 0.0021F + 0.7F);
		float slow = Mth.sin(time * 0.0009F + 2.1F);
		return Mth.clamp(0.5F + 0.28F * fast + 0.22F * slow, 0.0F, 1.0F);
	}

	private static float smooth(float t) {
		t = Mth.clamp(t, 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	/** The band the background wind is currently sampling from. */
	public WindClimate.Range currentRange(MinecraftServer server) {
		return WindClimate.range(server.overworld(), this.season);
	}

	/** Hook for a future season system. */
	public void setSeasonProfile(WindClimate.SeasonProfile season) {
		this.season = season;
	}

	/** Active event name for /wind, or null when the wind is undisturbed. */
	public String eventLabel() {
		return this.event == null ? null : this.event.displayName();
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

	/**
	 * Return to a clean startup state. Called when a server starts or stops so a
	 * pinned wind or an event schedule cannot leak into the next world opened in
	 * the same client process.
	 */
	public void reset() {
		this.season = WindClimate.SeasonProfile.TEMPERATE;
		this.angle = 90.0F;
		this.strength = 0.05F;
		this.targetAngle = 90.0F;
		this.targetStrength = 0.05F;
		this.pinned = false;
		this.nextDirection = 0L;
		this.nextStrength = 0L;
		this.scheduled = false;
		this.nextEvent = 0L;
		this.event = null;
		this.eventStart = 0L;
		this.eventEnd = 0L;
		this.eventPeak = 0.0F;
		this.eventVeer = 0.0F;
		this.previousRain = 0.0F;
		this.previousThunder = 0.0F;
		this.weatherSeen = false;
	}

	/** Pin the wind (used by /wind set and tests). */
	public void set(float angle, float strength) {
		this.angle = Mth.wrapDegrees(angle);
		this.targetAngle = this.angle;
		this.strength = Mth.clamp(strength, 0.0F, 1.0F);
		this.targetStrength = this.strength;
		this.pinned = true;
		this.event = null;
		this.scheduled = false;
		this.weatherSeen = false;
		this.nextDirection = 0L;
		this.nextStrength = 0L;
	}

	/** Release the wind back to its natural drift. */
	public void unpin() {
		this.pinned = false;
		this.event = null;
		this.scheduled = false;
		this.weatherSeen = false;
		this.nextEvent = 0L;
		this.nextDirection = 0L;
		this.nextStrength = 0L;
	}

	/** Force an event now, e.g. from /wind event; also releases a pinned wind. */
	public void forceEvent(WindEvent type, ServerLevel level) {
		this.pinned = false;
		this.weatherSeen = false;
		this.scheduled = true;
		this.nextDirection = 0L;
		this.nextStrength = 0L;
		this.startEvent(type, level.getGameTime(), level.getRandom(), "manual", true);
	}

	public boolean pinned() {
		return this.pinned;
	}
}
