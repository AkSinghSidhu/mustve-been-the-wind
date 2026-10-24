package com.windsmoke.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Wind-driven replacement for the vanilla campfire smoke. The vanilla call
 * spawns particles with no horizontal velocity, which is why the plume always
 * rose straight up; here the puff inherits the current wind vector.
 */
public final class CampfireSmoke {
	private CampfireSmoke() {
	}

	public static void spawn(Level level, BlockPos pos, boolean signalFire) {
		float density = WindSmokeConfig.smokeDensity();
		if (density <= 0.0F) {
			return;
		}

		Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
		double maxDistance = WindSmokeConfig.effectDistanceBlocks();
		if (camera.distanceToSqr(Vec3.atCenterOf(pos)) > maxDistance * maxDistance) {
			return;
		}

		RandomSource random = level.getRandom();
		float strength = WindSmokeConfig.visualStrength();
		double radians = Math.toRadians(WindClientState.angle());
		double windX = Math.sin(radians) * strength;
		double windZ = -Math.cos(radians) * strength;
		SimpleParticleType type = signalFire ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;

		int count = Math.max(1, Math.round((3 + random.nextInt(3)) * density));
		for (int i = 0; i < count; i++) {
			level.addAlwaysVisibleParticle(type, true,
				pos.getX() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
				pos.getY() + random.nextDouble() + random.nextDouble(),
				pos.getZ() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
				windX * 0.08,
				0.07,
				windZ * 0.08);
		}
	}
}
