package com.windsmoke.client.mixin.sodium;

import com.windsmoke.client.WindSway;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Translucent quads can be deferred by Sodium's sorter. Tag their vertices
 * before the collector copies them so a later deferred write keeps the wind
 * code as well as an immediate push.
 */
@Mixin(value = TranslucentGeometryCollector.class, remap = false)
public abstract class SodiumTranslucentGeometryCollectorMixin {
	@Inject(
		method = "appendQuad([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/model/quad/properties/ModelQuadFacing;I)Z",
		at = @At("HEAD")
	)
	private void windsmoke$tagVertices(
		ChunkVertexEncoder.Vertex[] vertices, ModelQuadFacing facing, int normal, CallbackInfoReturnable<Boolean> cir
	) {
		for (int i = 0; i < vertices.length; i++) {
			ChunkVertexEncoder.Vertex vertex = vertices[i];
			vertex.color = WindSway.tagCurrent(vertex.color, vertex.y);
		}
	}
}
