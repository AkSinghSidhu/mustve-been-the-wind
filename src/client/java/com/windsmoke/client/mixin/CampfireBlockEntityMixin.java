package com.windsmoke.client.mixin;

import com.windsmoke.client.CampfireSmoke;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CampfireBlockEntity.class)
public class CampfireBlockEntityMixin {
	/**
	 * Replace the vanilla no-velocity smoke entirely (instead of adding on top
	 * of it), so the whole plume obeys the wind.
	 */
	@Redirect(
		method = "particleTick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/CampfireBlock;makeParticles(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ZZ)V"
		)
	)
	private static void windsmoke$windDrivenSmoke(Level level, BlockPos pos, boolean signalFire, boolean small) {
		CampfireSmoke.spawn(level, pos, signalFire);
	}
}
