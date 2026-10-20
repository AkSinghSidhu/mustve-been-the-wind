package com.windsmoke.client.mixin;

import com.windsmoke.client.WindBannerCloth;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starts a new banner-geometry frame. All pattern layers and render passes for
 * the same frame then share one cloth step and one set of normals per banner.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void windsmoke$beginBannerFrame(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
		WindBannerCloth.beginFrame();
	}
}
