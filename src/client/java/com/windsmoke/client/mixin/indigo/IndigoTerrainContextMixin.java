package com.windsmoke.client.mixin.indigo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.windsmoke.client.WindSway;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's Indigo renderer writes chunk geometry itself and never calls
 * {@code BlockRenderDispatcher.renderBatched}, so the vanilla hook is not
 * enough. This tracks the block currently being emitted and wraps the
 * per-layer consumer so the same alpha codes are written.
 */
@Mixin(value = TerrainRenderContext.class, remap = false)
public abstract class IndigoTerrainContextMixin {
	@Inject(method = "bufferModel", at = @At("HEAD"), remap = false)
	private void windsmoke$beginBlock(BlockStateModel model, BlockState state, BlockPos pos, CallbackInfo ci) {
		WindSway.begin(state, pos);
	}

	@Inject(method = "bufferModel", at = @At("RETURN"), remap = false)
	private void windsmoke$endBlock(BlockStateModel model, BlockState state, BlockPos pos, CallbackInfo ci) {
		WindSway.end();
	}

	@ModifyReturnValue(method = "getVertexConsumer", at = @At("RETURN"), remap = false)
	private VertexConsumer windsmoke$wrapConsumer(VertexConsumer original) {
		return WindSway.wrapCurrent(original);
	}
}
