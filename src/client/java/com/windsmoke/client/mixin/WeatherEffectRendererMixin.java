package com.windsmoke.client.mixin;

import com.windsmoke.client.WindVisuals;
import java.util.List;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.WeatherEffectRenderer.ColumnInstance;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leans rain and snow columns downwind. Each precipitation column is a
 * camera-facing quad built from four vertices (top-left, top-right,
 * bottom-right, bottom-left), so the top two vertices are shifted upwind and
 * the streak trails behind the falling drop.
 */
@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {
	@Unique
	private static int windsmoke$vertexIndex;

	@Inject(method = "renderInstances", at = @At("HEAD"))
	private void windsmoke$resetVertexIndex(VertexConsumer vertexConsumer, List<ColumnInstance> columns,
			Vec3 camera, float brightness, int radius, float intensity, CallbackInfo ci) {
		windsmoke$vertexIndex = 0;
	}

	@Redirect(
		method = "renderInstances",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
		)
	)
	private VertexConsumer windsmoke$lean(VertexConsumer vertexConsumer, float x, float y, float z) {
		int vertex = windsmoke$vertexIndex++ & 3;
		if (vertex == 0 || vertex == 1) {
			x += WindVisuals.precipitationLeanX();
			z += WindVisuals.precipitationLeanZ();
		}
		return vertexConsumer.addVertex(x, y, z);
	}
}
