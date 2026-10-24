package com.windsmoke.client.mixin;

import com.windsmoke.client.WindSmokeConfig;
import com.windsmoke.client.WindVisuals;
import net.minecraft.client.particle.FallingLeavesParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Adds a downwind displacement to vanilla falling-leaf particles as they move.
 * The offset is applied to the movement delta rather than the stored velocity,
 * so the particle keeps its authored flutter and cannot run away.
 */
@Mixin(Particle.class)
public class ParticleMixin {
	@ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, index = 1)
	private double windsmoke$windX(double x) {
		if (!((Object)this instanceof FallingLeavesParticle)) {
			return x;
		}

		return x + WindVisuals.windX() * 0.07 * WindSmokeConfig.visualStrength();
	}

	@ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, index = 3)
	private double windsmoke$windZ(double z) {
		if (!((Object)this instanceof FallingLeavesParticle)) {
			return z;
		}

		return z + WindVisuals.windZ() * 0.07 * WindSmokeConfig.visualStrength();
	}
}
