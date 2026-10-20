package com.windsmoke.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Stable identity for a world banner.
 *
 * <p>The old implementation packed a truncated position hash into the banner
 * render-state phase. Two nearby banners could collide after the float
 * rounding. Here the full dimension key and packed block position are used as
 * the map key, and a small integer token is what actually travels through the
 * vanilla render state. Tokens are unique for the lifetime of the client, so
 * cloth state cannot be shared by two banners.
 */
public final class BannerIdentity {
	private static final ConcurrentHashMap<Key, Integer> IDS = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Integer, Entry> ENTRIES = new ConcurrentHashMap<>();
	private static final AtomicInteger NEXT = new AtomicInteger();

	private BannerIdentity() {
	}

	public static int token(Level level, BlockPos pos) {
		ResourceKey<Level> dimension = level == null ? null : level.dimension();
		Key key = new Key(dimension, pos.asLong());
		Integer existing = IDS.get(key);
		if (existing != null) {
			return existing;
		}

		int token = NEXT.getAndIncrement();
		Integer raced = IDS.putIfAbsent(key, token);
		if (raced != null) {
			return raced;
		}
		ENTRIES.put(token, new Entry(dimension, pos.immutable()));
		return token;
	}

	public static void clear() {
		IDS.clear();
		ENTRIES.clear();
		NEXT.set(0);
	}

	public static boolean isNear(int token, double maxDistance) {
		if (maxDistance <= 0.0) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return false;
		}

		Entry entry = ENTRIES.get(token);
		if (entry == null || entry.dimension() == null
			|| !entry.dimension().equals(minecraft.level.dimension())) {
			return false;
		}

		Vec3 camera = minecraft.gameRenderer.getMainCamera().position();
		return camera.distanceToSqr(Vec3.atCenterOf(entry.pos())) <= maxDistance * maxDistance;
	}

	private record Key(ResourceKey<Level> dimension, long packedPos) {
	}

	private record Entry(ResourceKey<Level> dimension, BlockPos pos) {
	}
}
