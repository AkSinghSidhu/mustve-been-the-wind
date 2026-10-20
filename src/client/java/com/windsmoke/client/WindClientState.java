package com.windsmoke.client;

import net.minecraft.util.Mth;

/**
 * The wind as last reported by the server, smoothly interpolated on the client.
 *
 * <p>Updates arrive four times a second. Snapping straight to each packet made
 * vegetation, banners and clouds visibly step. Each update now starts a short
 * transition from the currently displayed value to the new one, taking the
 * shortest path around the compass.
 */
public final class WindClientState {
	private static final long TRANSITION_NANOS = 250_000_000L;
	private static final float DEFAULT_ANGLE = 90.0F;
	private static final float DEFAULT_STRENGTH = 0.3F;

	private static volatile float fromAngle = DEFAULT_ANGLE;
	private static volatile float toAngle = DEFAULT_ANGLE;
	private static volatile float fromStrength = DEFAULT_STRENGTH;
	private static volatile float toStrength = DEFAULT_STRENGTH;
	private static volatile long transitionStartNanos;
	private static volatile boolean initialized;

	private WindClientState() {
	}

	public static void set(float newAngle, float newStrength) {
		float wrapped = Mth.wrapDegrees(newAngle);
		if (!initialized) {
			initialized = true;
			fromAngle = wrapped;
			toAngle = wrapped;
			fromStrength = newStrength;
			toStrength = newStrength;
			transitionStartNanos = System.nanoTime();
			return;
		}

		fromAngle = angle();
		fromStrength = strength();
		toAngle = wrapped;
		toStrength = newStrength;
		transitionStartNanos = System.nanoTime();
	}

	public static float angle() {
		if (!initialized) {
			return DEFAULT_ANGLE;
		}
		float progress = transitionProgress();
		if (progress >= 1.0F) {
			return toAngle;
		}
		float delta = Mth.wrapDegrees(toAngle - fromAngle);
		return Mth.wrapDegrees(fromAngle + delta * progress);
	}

	public static float strength() {
		if (!initialized) {
			return DEFAULT_STRENGTH;
		}
		float progress = transitionProgress();
		if (progress >= 1.0F) {
			return toStrength;
		}
		return Mth.lerp(progress, fromStrength, toStrength);
	}

	/** Reset when leaving a world so one server's weather cannot leak into the next. */
	public static void reset() {
		initialized = false;
		fromAngle = DEFAULT_ANGLE;
		toAngle = DEFAULT_ANGLE;
		fromStrength = DEFAULT_STRENGTH;
		toStrength = DEFAULT_STRENGTH;
		transitionStartNanos = 0L;
	}

	private static float transitionProgress() {
		long start = transitionStartNanos;
		if (start == 0L) {
			return 1.0F;
		}
		return Mth.clamp((System.nanoTime() - start) / (float)TRANSITION_NANOS, 0.0F, 1.0F);
	}
}
