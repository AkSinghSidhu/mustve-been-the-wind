package com.windsmoke.client;

/** The wind as last reported by the server. */
public final class WindClientState {
	private static volatile float angle = 90.0F;
	private static volatile float strength = 0.3F;

	private WindClientState() {
	}

	public static void set(float newAngle, float newStrength) {
		angle = newAngle;
		strength = newStrength;
	}

	public static float angle() {
		return angle;
	}

	public static float strength() {
		return strength;
	}
}
