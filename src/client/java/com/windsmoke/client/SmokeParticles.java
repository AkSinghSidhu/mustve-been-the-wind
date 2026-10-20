package com.windsmoke.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Extra campfire smoke pushed along the wind vector, so the plume leans
 * downwind. Campfire positions are rescanned once a second; particles are
 * spawned along a curve that bends further with height.
 */
public final class SmokeParticles {
	private static final int SCAN_RADIUS = 3;
	private static final double SPAWN_RADIUS = 48.0;
	private static final List<BlockPos> CAMPFIRES = new ArrayList<>();
	private static long lastScan = Long.MIN_VALUE;

	private SmokeParticles() {
	}

	public static void tick(Minecraft client) {
		ClientLevel level = client.level;
		Player player = client.player;
		if (level == null || player == null) {
			CAMPFIRES.clear();
			return;
		}

		long time = level.getGameTime();
		if (time < lastScan || time - lastScan >= 20L) {
			lastScan = time;
			scan(level, player);
		}

		float strength = WindClientState.strength();
		if (strength <= 0.02F || CAMPFIRES.isEmpty()) {
			return;
		}

		double radians = Math.toRadians(WindClientState.angle());
		double windX = Math.sin(radians) * strength;
		double windZ = -Math.cos(radians) * strength;
		RandomSource random = level.getRandom();
		BlockPos playerPos = player.blockPosition();

		for (BlockPos pos : CAMPFIRES) {
			if (pos.distSqr(playerPos) > SPAWN_RADIUS * SPAWN_RADIUS) {
				continue;
			}
			if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
				continue;
			}

			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof CampfireBlock) || !state.getValue(CampfireBlock.LIT)) {
				continue;
			}

			int puffs = 1 + random.nextInt(2);
			for (int i = 0; i < puffs; i++) {
				double height = 0.5 + random.nextDouble() * 2.4;
				double bend = 0.55 * height;
				double x = pos.getX() + 0.5 + windX * bend + (random.nextDouble() - 0.5) * 0.3;
				double y = pos.getY() + 1.0 + height * 0.55 + random.nextDouble() * 0.2;
				double z = pos.getZ() + 0.5 + windZ * bend + (random.nextDouble() - 0.5) * 0.3;
				level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z,
					windX * 0.015, 0.005 + random.nextDouble() * 0.01, windZ * 0.015);
			}
		}
	}

	private static void scan(ClientLevel level, Player player) {
		CAMPFIRES.clear();
		int centerX = player.chunkPosition().x;
		int centerZ = player.chunkPosition().z;

		for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
			for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(centerX + dx, centerZ + dz);
				if (chunk == null) {
					continue;
				}

				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					if (blockEntity instanceof CampfireBlockEntity) {
						CAMPFIRES.add(blockEntity.getBlockPos().immutable());
					}
				}
			}
		}
	}
}
