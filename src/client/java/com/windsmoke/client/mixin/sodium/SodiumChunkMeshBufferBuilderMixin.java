package com.windsmoke.client.mixin.sodium;

import com.windsmoke.client.WindSway;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tags Sodium's packed block vertices just before they are written. Sodium
 * stores colours as ABGR, so changing the high byte leaves RGB untouched and
 * lets the patched vertex shader decode the wind category from alpha.
 */
@Mixin(ChunkMeshBufferBuilder.class)
public abstract class SodiumChunkMeshBufferBuilderMixin {
	@Inject(
		method = "push([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)V",
		at = @At("HEAD")
	)
	private void windsmoke$tagVertices(ChunkVertexEncoder.Vertex[] vertices, int materialBits, CallbackInfo ci) {
		for (int i = 0; i < vertices.length; i++) {
			ChunkVertexEncoder.Vertex vertex = vertices[i];
			vertex.color = WindSway.tagCurrent(vertex.color, vertex.y);
		}
	}
}
