package com.windsmoke.client.mixin;

import com.windsmoke.client.WindVisuals;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CloudRenderer.class)
public class CloudRendererMixin {
	/**
	 * Offset the camera position used for cloud sampling by the accumulated
	 * wind drift. Shifting the camera moves the cloud plane the other way, so
	 * {@link WindVisuals} accumulates a negative offset.
	 */
	@ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Vec3 windsmoke$windDrivenClouds(Vec3 camera) {
		return camera.add(WindVisuals.cloudOffsetX(), 0.0, WindVisuals.cloudOffsetZ());
	}

	/**
	 * Freeze the vanilla constant eastward drift; all cloud motion now comes
	 * from the wind, so a dead calm really is calm.
	 */
	@ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private long windsmoke$disableVanillaCloudDrift(long gameTime) {
		return 0L;
	}
}
