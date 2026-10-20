package com.windsmoke.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.windsmoke.client.WindSway;
import java.util.List;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Wraps the block model consumer for wind-sensitive blocks. Chunk building may
 * happen on worker threads, so {@link WindSway} keeps its wrapper thread-local.
 */
@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {
	@Redirect(
		method = "renderBatched",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZI)V"
		)
	)
	private void windsmoke$swayTesselation(
		ModelBlockRenderer renderer,
		BlockAndTintGetter getter,
		List<BlockModelPart> parts,
		BlockState state,
		BlockPos pos,
		PoseStack poseStack,
		VertexConsumer vertexConsumer,
		boolean cull,
		int overlay
	) {
		renderer.tesselateBlock(
			getter,
			parts,
			state,
			pos,
			poseStack,
			WindSway.wrap(state, pos, vertexConsumer),
			cull,
			overlay
		);
	}
}
