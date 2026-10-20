package com.windsmoke;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Campfire smoke carries downwind. Every second, hostile mobs standing in the
 * smoke's path get nudged toward the fire.
 */
public final class SmokeAttraction {
	private static final int CHUNK_RADIUS = 3;
	private static final double ATTRACT_RADIUS = 24.0;
	/** How far into the downwind half-space a mob must be (cosine of the cone). */
	private static final double DOWNWIND_COS = 0.35;

	private SmokeAttraction() {
	}

	public static void tick(MinecraftServer server, WindState wind) {
		ServerLevel level = server.overworld();
		double windX = wind.windX();
		double windZ = wind.windZ();

		for (ServerPlayer player : level.players()) {
			int centerX = player.chunkPosition().x;
			int centerZ = player.chunkPosition().z;

			for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
				for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
					LevelChunk chunk = level.getChunkSource().getChunkNow(centerX + dx, centerZ + dz);
					if (chunk == null) {
						continue;
					}

					for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
						if (!(blockEntity instanceof CampfireBlockEntity)) {
							continue;
						}

						BlockState state = blockEntity.getBlockState();
						if (!state.hasProperty(CampfireBlock.LIT) || !state.getValue(CampfireBlock.LIT)) {
							continue;
						}

						attract(level, blockEntity.getBlockPos(), windX, windZ);
					}
				}
			}
		}
	}

	private static void attract(ServerLevel level, BlockPos pos, double windX, double windZ) {
		Vec3 center = Vec3.atCenterOf(pos);
		AABB box = new AABB(pos).inflate(ATTRACT_RADIUS);
		List<Monster> monsters = level.getEntitiesOfClass(Monster.class, box,
			monster -> !monster.isDeadOrDying() && monster.getTarget() == null);

		for (Monster monster : monsters) {
			Vec3 delta = center.subtract(monster.position());
			double distance = delta.length();
			if (distance < 2.5 || distance > ATTRACT_RADIUS) {
				continue;
			}

			double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
			if (horizontal < 0.001) {
				continue;
			}

			// The mob must lie downwind of the fire for the smoke to reach it.
			double alignment = (delta.x * windX + delta.z * windZ) / horizontal;
			if (alignment < DOWNWIND_COS) {
				continue;
			}

			if (monster.getNavigation().isDone() || monster.tickCount % 40 == 0) {
				monster.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.7);
			}
		}
	}
}
