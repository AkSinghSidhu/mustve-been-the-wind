package com.windsmoke.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.windsmoke.client.BannerFlagAccess;
import com.windsmoke.client.WindBannerCloth;
import net.minecraft.client.model.Model;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla cube flag with a continuously deforming cloth when the
 * banner is rendered in the world. A phase of 10.0 or above is the marker set
 * by {@code BannerRendererMixin}; inventory previews use phase 0 and render the
 * original model.
 */
@Mixin(Model.class)
public abstract class ModelMixin {
	@Inject(
		method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void windsmoke$renderBannerCloth(
		PoseStack poseStack,
		VertexConsumer vertexConsumer,
		int light,
		int overlay,
		int color,
		CallbackInfo ci
	) {
		if (this instanceof BannerFlagAccess access && access.windsmoke$phase() >= WindBannerCloth.WORLD_PHASE) {
			WindBannerCloth.render(poseStack, vertexConsumer, light, overlay, color, access);
			ci.cancel();
		}
	}
}
