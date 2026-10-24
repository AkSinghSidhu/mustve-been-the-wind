package com.windsmoke;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Campfire smoke carries downwind. Once a second, hostile mobs standing in the
 * exposed smoke path get nudged toward the fire.
 *
 * <p>The scan is chunk-deduplicated across players, has a wind-strength gate, a
 * line-of-sight check, and a bounded number of pathfinding requests. It is also
 * an experimental gameplay feature that can be disabled in the server config or
 * with {@code /wind smoke false}.
 */
public final class SmokeAttraction {
	private static final int CHUNK_RADIUS = 3;
	private static final double MIN_WIND_STRENGTH = 0.05;
	/** How far into the downwind half-space a mob must be (cosine of the cone). */
	private static final double DOWNWIND_COS = 0.35;
	private static final int PATHFINDING_BUDGET = 256;
	private static final int MAX_CHUNKS_PER_SCAN = 96;
	private static int chunkCursor;

	private SmokeAttraction() {
	}

	public static void tick(MinecraftServer server, WindState wind) {
		if (!WindSmokeServerConfig.smokeAttractsMobs() || wind.strength() < MIN_WIND_STRENGTH) {
			return;
		}

		ServerLevel level = server.overworld();
		double windX = wind.windX();
		double windZ = wind.windZ();
		double radius = WindSmokeServerConfig.smokeAttractionRadius();

		// Players often stand in overlapping chunk radii. Collect each chunk once
		// instead of scanning the same campfire several times per second.
		LongOpenHashSet chunks = new LongOpenHashSet();
		for (ServerPlayer player : level.players()) {
			int centerX = player.chunkPosition().x();
			int centerZ = player.chunkPosition().z();

			for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
				for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
					chunks.add(ChunkPos.pack(centerX + dx, centerZ + dz));
				}
			}
		}

		// Do not scan an unbounded set of chunks in one server tick. Rotate
		// through the unique set on successive seconds; small areas still scan
		// every pass, while a crowded server spreads the work out.
		long[] sorted = chunks.toLongArray();
		Arrays.sort(sorted);
		int start = sorted.length == 0 ? 0 : Math.floorMod(chunkCursor, sorted.length);
		int processed = 0;
		int pathBudget = PATHFINDING_BUDGET;

		for (int index = 0; index < sorted.length && processed < MAX_CHUNKS_PER_SCAN; index++, processed++) {
			long packedChunk = sorted[(start + index) % sorted.length];
			LevelChunk chunk = level.getChunkSource().getChunkNow(
				ChunkPos.getX(packedChunk), ChunkPos.getZ(packedChunk));
			if (chunk == null) {
				continue;
			}

			for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
				if (pathBudget <= 0) {
					chunkCursor = (start + processed + 1) % Math.max(1, sorted.length);
					return;
				}
				if (!(blockEntity instanceof CampfireBlockEntity)) {
					continue;
				}

				BlockState state = blockEntity.getBlockState();
				if (!state.hasProperty(CampfireBlock.LIT) || !state.getValue(CampfireBlock.LIT)) {
					continue;
				}

				pathBudget = attract(level, blockEntity.getBlockPos(), windX, windZ, radius, pathBudget);
			}
		}

		chunkCursor = sorted.length == 0 ? 0 : (start + processed) % sorted.length;
	}

	private static int attract(
		ServerLevel level,
		BlockPos pos,
		double windX,
		double windZ,
		double radius,
		int pathBudget
	) {
		Vec3 center = Vec3.atCenterOf(pos);
		AABB box = new AABB(pos).inflate(radius);
		List<Monster> monsters = level.getEntitiesOfClass(Monster.class, box,
			monster -> !monster.isDeadOrDying() && monster.getTarget() == null);

		for (Monster monster : monsters) {
			if (pathBudget <= 0) {
				break;
			}

			Vec3 toMob = monster.position().subtract(center);
			double distance = toMob.length();
			if (distance < 2.5 || distance > radius) {
				continue;
			}

			double horizontal = Math.sqrt(toMob.x * toMob.x + toMob.z * toMob.z);
			if (horizontal < 0.001) {
				continue;
			}

			// The mob must lie downwind of the fire for the smoke to reach it.
			double alignment = (toMob.x * windX + toMob.z * windZ) / horizontal;
			if (alignment < DOWNWIND_COS) {
				continue;
			}

			// Do not pull mobs through a wall or roof. Start just above the fire
			// where the plume exists, then test the actual smoke path.
			Vec3 smokeSource = center.add(0.0, 0.8, 0.0);
			BlockHitResult obstruction = level.clip(new ClipContext(
				smokeSource,
				monster.getEyePosition(),
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				monster
			));
			if (obstruction.getType() != HitResult.Type.MISS) {
				continue;
			}

			if (!monster.getNavigation().isDone() && monster.tickCount % 40 != 0) {
				continue;
			}

			// Stop a couple of blocks short so mobs investigate from a distance
			// instead of walking into the flames and burning to death.
			double stop = 2.0;
			double targetX = pos.getX() + 0.5 + windX * stop;
			double targetZ = pos.getZ() + 0.5 + windZ * stop;

			if (Boolean.getBoolean("windsmoke.testharness")) {
				WindSmoke.LOGGER.info("[windsmoke] nudge {} fire={} mob=({}, {}, {}) dist={} align={} target=({}, {})",
					monster.getName().getString(), pos.toShortString(),
					String.format("%.1f", monster.getX()), String.format("%.1f", monster.getY()),
					String.format("%.1f", monster.getZ()), String.format("%.1f", distance),
					String.format("%.2f", alignment), String.format("%.1f", targetX), String.format("%.1f", targetZ));
			}

			monster.getNavigation().moveTo(targetX, pos.getY(), targetZ, 0.7);
			pathBudget--;
		}

		return pathBudget;
	}
}
