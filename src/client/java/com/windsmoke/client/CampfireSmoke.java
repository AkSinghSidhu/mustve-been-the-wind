package com.windsmoke.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Wind-driven replacement for the vanilla campfire smoke. The vanilla call
 * spawns particles with no horizontal velocity, which is why the plume always
 * rose straight up; here the puff inherits the current wind vector.
 */
public final class CampfireSmoke {
	private CampfireSmoke() {
	}

	public static void spawn(Level level, BlockPos pos, boolean signalFire) {
		RandomSource random = level.getRandom();
		float strength = WindClientState.strength();
		double radians = Math.toRadians(WindClientState.angle());
		double windX = Math.sin(radians) * strength;
		double windZ = -Math.cos(radians) * strength;
		SimpleParticleType type = signalFire ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;

		int count = 3 + random.nextInt(3);
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
