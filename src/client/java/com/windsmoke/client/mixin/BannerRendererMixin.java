package com.windsmoke.client.mixin;

import com.windsmoke.client.BannerIdentity;
import com.windsmoke.client.WindBannerCloth;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla per-banner phase with a stable identity token and a
 * whole-number marker. The marker distinguishes world banners (which get the
 * simulated cloth) from item/inventory previews (which keep the vanilla model).
 * The token comes from the full dimension + packed block position, so two
 * distinct banners can never share cloth simulation state after float rounding.
 */
@Mixin(BannerRenderer.class)
public class BannerRendererMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void windsmoke$markWorldBanner(
		BannerBlockEntity bannerBlockEntity,
		BannerRenderState bannerRenderState,
		float partialTick,
		Vec3 camera,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
		CallbackInfo ci
	) {
		int token = BannerIdentity.token(bannerBlockEntity.getLevel(), bannerBlockEntity.getBlockPos());
		bannerRenderState.phase = WindBannerCloth.WORLD_PHASE + token;
	}
}
