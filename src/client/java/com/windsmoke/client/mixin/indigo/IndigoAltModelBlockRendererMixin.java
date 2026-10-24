package com.windsmoke.client.mixin.indigo;

import com.windsmoke.client.WindSway;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tags terrain quads emitted by Fabric's Indigo renderer.
 *
 * <p>Fabric API replaces the vanilla section-compiler model call with
 * {@link AltModelBlockRendererImpl}, so {@code ModelBlockRendererMixin} never
 * sees terrain quads while Indigo is active. Indigo pushes this renderer as a
 * quad transform; the transform runs after shading and tinting, which is the
 * right point to encode the wind code in the vertex alpha.
 */
@Mixin(value = AltModelBlockRendererImpl.class, remap = false)
public abstract class IndigoAltModelBlockRendererMixin {
	@Shadow
	@Final
	private Vector3f offset;

	@Inject(method = "tesselateBlock", at = @At("HEAD"), remap = false)
	private void windsmoke$beginBlock(
		QuadEmitter emitter,
		float x,
		float y,
		float z,
		BlockAndTintGetter level,
		BlockPos pos,
		BlockState state,
		BlockStateModel model,
		long seed,
		CallbackInfo ci
	) {
		WindSway.begin(state, pos);
	}

	@Inject(method = "tesselateBlock", at = @At("RETURN"), remap = false)
	private void windsmoke$endBlock(
		QuadEmitter emitter,
		float x,
		float y,
		float z,
		BlockAndTintGetter level,
		BlockPos pos,
		BlockState state,
		BlockStateModel model,
		long seed,
		CallbackInfo ci
	) {
		WindSway.end();
	}

	@Inject(
		method = "transform",
		at = @At(
			value = "INVOKE",
			target = "Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/MutableQuadView;translate(FFF)Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/MutableQuadView;"
		),
		remap = false
	)
	private void windsmoke$tagQuad(MutableQuadView quad, CallbackInfoReturnable<Boolean> cir) {
		WindSway.tagCurrentIndigoQuad(quad, this.offset.y);
	}
}
