package com.windsmoke.client.mixin;

import com.mojang.blaze3d.vertex.QuadInstance;
import com.windsmoke.client.WindSway;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tags the 26.2 {@link QuadInstance} colours while a block is being tesselated.
 *
 * <p>26.2 no longer routes block tesselation through
 * {@code BlockRenderDispatcher}, and Fabric's Indigo support already redirects
 * the section-compiler call. Hooking the shared renderer instead keeps both
 * paths working: the thread-local context identifies the current block, and
 * the quad sink is left untouched.
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
	@Inject(method = "tesselateBlock", at = @At("HEAD"))
	private void windsmoke$beginBlock(
		BlockQuadOutput output,
		float x,
		float y,
		float z,
		BlockAndTintGetter getter,
		BlockPos pos,
		BlockState state,
		BlockStateModel model,
		long seed,
		CallbackInfo ci
	) {
		WindSway.begin(state, pos);
	}

	@Inject(method = "tesselateBlock", at = @At("RETURN"))
	private void windsmoke$endBlock(
		BlockQuadOutput output,
		float x,
		float y,
		float z,
		BlockAndTintGetter getter,
		BlockPos pos,
		BlockState state,
		BlockStateModel model,
		long seed,
		CallbackInfo ci
	) {
		WindSway.end();
	}

	@Redirect(
		method = "putQuadWithTint",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"
		)
	)
	private void windsmoke$tagQuad(
		BlockQuadOutput output,
		float x,
		float y,
		float z,
		BakedQuad quad,
		QuadInstance instance
	) {
		WindSway.tagCurrentQuad(y, quad, instance);
		output.put(x, y, z, quad, instance);
	}
}
