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

	private WindVisuals() {
	}

	public static void tick() {
		double strength = WindClientState.strength();
		double speed = CLOUD_BLOCKS_PER_TICK * strength;
		// Adding to the camera offset shifts the sampled cloud texture the
		// opposite way, hence the minus.
		cloudOffsetX -= windX() * speed;
		cloudOffsetZ -= windZ() * speed;
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
		return (float)(-windX() * PRECIPITATION_LEAN * WindClientState.strength());
	}

	/** Top-of-column lean along Z: upwind, so the streak trails behind the drop. */
	public static float precipitationLeanZ() {
		return (float)(-windZ() * PRECIPITATION_LEAN * WindClientState.strength());
	}
}
