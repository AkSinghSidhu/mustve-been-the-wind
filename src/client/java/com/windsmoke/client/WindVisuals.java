package com.windsmoke.client;

/**
 * Client-side visual state driven by the synced wind.
 *
 * <p>Clouds are rendered as an endlessly scrolling texture, so we accumulate an
 * offset every client tick and let the cloud renderer sample that offset. Rain
 * (and snow) is a set of vertical billboards; the renderer mixin shears their
 * top edge downwind.
 */
public final class WindVisuals {
	/** Blocks per tick of cloud drift at 100% strength (6 blocks/s). */
	private static final double CLOUD_BLOCKS_PER_TICK = 0.30;
	/** How far the top of a precipitation column leans at 100% strength. */
	private static final double PRECIPITATION_LEAN = 4.0;

	private static double cloudOffsetX;
	private static double cloudOffsetZ;
	private static float animationTime;

	private WindVisuals() {
	}

	public static void tick() {
		animationTime += 1.0F;
		double strength = WindSmokeConfig.visualStrength();
		double speed = CLOUD_BLOCKS_PER_TICK * strength;
		// Adding to the camera offset shifts the sampled cloud texture the
		// opposite way, hence the minus.
		cloudOffsetX -= windX() * speed;
		cloudOffsetZ -= windZ() * speed;
	}

	/** Client tick counter used by the block and banner sway animations. */
	public static float animationTime() {
		return animationTime;
	}

	/** Clear accumulated motion when the client leaves a world. */
	public static void reset() {
		cloudOffsetX = 0.0;
		cloudOffsetZ = 0.0;
		animationTime = 0.0F;
	}

	public static double windX() {
		return Math.sin(Math.toRadians(WindClientState.angle()));
	}

	public static double windZ() {
		return -Math.cos(Math.toRadians(WindClientState.angle()));
	}

	public static double cloudOffsetX() {
		return cloudOffsetX;
	}

	public static double cloudOffsetZ() {
		return cloudOffsetZ;
	}

	/** Top-of-column lean along X: upwind, so the streak trails behind the drop. */
	public static float precipitationLeanX() {
		return (float)(-windX() * PRECIPITATION_LEAN * WindSmokeConfig.visualStrength());
	}

	/** Top-of-column lean along Z: upwind, so the streak trails behind the drop. */
	public static float precipitationLeanZ() {
		return (float)(-windZ() * PRECIPITATION_LEAN * WindSmokeConfig.visualStrength());
	}
}
