package com.windsmoke;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Weather and time-of-day wind ranges.
 *
 * <p>All values are fractions of the maximum wind (0-1). The base bands are:
 * <ul>
 *     <li>clear day: 2-8%, clear night: 1-5%</li>
 *     <li>rain: 25-50% by day, 28-55% at night (nocturnal jet)</li>
 *     <li>thunderstorm: 80-100% by day, 85-100% at night</li>
 * </ul>
 *
 * <p>Day/night is a smooth cosine, so dawn and dusk sit between the two bands
 * instead of switching abruptly. Rain and thunder levels are blended in, which
 * makes a storm ramp up and down with the weather. A {@link SeasonProfile} can
 * scale the whole table later; {@link SeasonProfile#TEMPERATE} is neutral and is
 * used until seasons exist.
 */
public final class WindClimate {
	/** A strength band. The live wind picks targets inside it. */
	public record Range(float min, float max) {
		public float sample(RandomSource random) {
			return Mth.lerp(random.nextFloat(), this.min, this.max);
		}

		public Range times(float scale) {
			return new Range(
				Mth.clamp(this.min * scale, 0.0F, 1.0F),
				Mth.clamp(this.max * scale, 0.0F, 1.0F));
		}
	}

	/**
	 * Seasonal hook. A future season system only needs to supply a profile
	 * (for example winter gales at 1.25x); the weather table stays untouched.
	 */
	public record SeasonProfile(float strengthScale) {
		public static final SeasonProfile TEMPERATE = new SeasonProfile(1.0F);
	}

	private static final Range CLEAR_DAY = new Range(0.02F, 0.08F);
	private static final Range CLEAR_NIGHT = new Range(0.01F, 0.05F);
	private static final Range RAIN_DAY = new Range(0.25F, 0.50F);
	private static final Range RAIN_NIGHT = new Range(0.28F, 0.55F);
	private static final Range THUNDER_DAY = new Range(0.80F, 1.00F);
	private static final Range THUNDER_NIGHT = new Range(0.85F, 1.00F);

	private WindClimate() {
	}

	public static Range range(ServerLevel level, SeasonProfile season) {
		float rain = Mth.clamp(level.getRainLevel(1.0F), 0.0F, 1.0F);
		float thunder = Mth.clamp(level.getThunderLevel(1.0F), 0.0F, 1.0F);
		float daylight = daylight(level.getDayTime());

		Range clear = lerp(CLEAR_NIGHT, CLEAR_DAY, daylight);
		Range wet = lerp(RAIN_NIGHT, RAIN_DAY, daylight);
		Range storm = lerp(THUNDER_NIGHT, THUNDER_DAY, daylight);

		Range range = lerp(lerp(clear, wet, rain), storm, thunder);
		return range.times(season.strengthScale());
	}

	/**
	 * 1.0 at noon, 0.0 at midnight, 0.5 at dawn/dusk. Minecraft day time 0 is
	 * sunrise, 6000 noon, 12000 sunset, 18000 midnight.
	 */
	public static float daylight(long dayTime) {
		double angle = ((dayTime % 24000L) / 24000.0 - 0.25) * Math.PI * 2.0;
		return (float)((Math.cos(angle) + 1.0) * 0.5);
	}

	private static Range lerp(Range a, Range b, float t) {
		return new Range(Mth.lerp(t, a.min(), b.min()), Mth.lerp(t, a.max(), b.max()));
	}
}
