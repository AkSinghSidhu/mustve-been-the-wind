package com.windsmoke.client.mixin.sodium;

import com.windsmoke.client.WindSway;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium's chunk renderer emits block models directly instead of going through
 * {@code BlockRenderDispatcher.renderBatched}. Track the block being rendered
 * so the Sodium vertex hooks can tag the vertices that belong to it.
 */
@Mixin(value = BlockRenderer.class, remap = false)
public abstract class SodiumBlockRendererMixin {
	@Inject(method = "renderModel", at = @At("HEAD"))
	private void windsmoke$beginBlock(
		BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci
	) {
		WindSway.begin(state, pos);
	}

	@Inject(method = "renderModel", at = @At("RETURN"))
	private void windsmoke$endBlock(
		BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci
	) {
		WindSway.end();
	}
}
