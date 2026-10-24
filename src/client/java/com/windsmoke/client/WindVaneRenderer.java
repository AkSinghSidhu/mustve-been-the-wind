package com.windsmoke.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.windsmoke.WindSmoke;
import com.windsmoke.block.WindVaneBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the rotating vane arrow above the copper post. The texture is a flat
 * arrow, so two perpendicular quads keep it readable from both eye level and
 * from above.
 */
@Environment(EnvType.CLIENT)
public final class WindVaneRenderer implements BlockEntityRenderer<WindVaneBlockEntity, WindVaneRenderer.State> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
		WindSmoke.MOD_ID, "textures/entity/wind_vane_arrow.png"
	);
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
	private static final float MIN_X = -0.46F;
	private static final float MAX_X = 0.46F;
	private static final float MIN_Y = 0.70F;
	private static final float MAX_Y = 1.18F;
	private static final float HORIZONTAL_Y = 0.95F;
	private static final float HALF_WIDTH = 0.12F;

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(
		WindVaneBlockEntity blockEntity,
		State state,
		float partialTick,
		Vec3 cameraPosition,
		ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay
	) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, crumblingOverlay);
		state.angle = WindClientState.angle();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.0F, 0.5F);
		// The texture points along local +X. At 0 degrees the wind blows north
		// (-Z), so a 90 degree offset maps the texture onto the wind vector.
		poseStack.mulPose(Axis.YP.rotationDegrees(90.0F - state.angle));
		collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, vertices) -> {
			verticalQuad(pose, vertices, state.lightCoords);
			horizontalQuad(pose, vertices, state.lightCoords);
		});
		poseStack.popPose();
	}

	private static void verticalQuad(PoseStack.Pose pose, VertexConsumer vertices, int light) {
		vertex(vertices, pose, MIN_X, MIN_Y, 0.0F, 0.0F, 1.0F, light, 0.0F, 0.0F, 1.0F);
		vertex(vertices, pose, MAX_X, MIN_Y, 0.0F, 1.0F, 1.0F, light, 0.0F, 0.0F, 1.0F);
		vertex(vertices, pose, MAX_X, MAX_Y, 0.0F, 1.0F, 0.0F, light, 0.0F, 0.0F, 1.0F);
		vertex(vertices, pose, MIN_X, MAX_Y, 0.0F, 0.0F, 0.0F, light, 0.0F, 0.0F, 1.0F);
	}

	private static void horizontalQuad(PoseStack.Pose pose, VertexConsumer vertices, int light) {
		float y = HORIZONTAL_Y;
		vertex(vertices, pose, MIN_X, y, -HALF_WIDTH, 0.0F, 1.0F, light, 0.0F, 1.0F, 0.0F);
		vertex(vertices, pose, MAX_X, y, -HALF_WIDTH, 1.0F, 1.0F, light, 0.0F, 1.0F, 0.0F);
		vertex(vertices, pose, MAX_X, y, HALF_WIDTH, 1.0F, 0.0F, light, 0.0F, 1.0F, 0.0F);
		vertex(vertices, pose, MIN_X, y, HALF_WIDTH, 0.0F, 0.0F, light, 0.0F, 1.0F, 0.0F);
	}

	private static void vertex(
		VertexConsumer vertices,
		PoseStack.Pose pose,
		float x,
		float y,
		float z,
		float u,
		float v,
		int light,
		float normalX,
		float normalY,
		float normalZ
	) {
		vertices.addVertex(pose, x, y, z)
			.setColor(0xFFFFFFFF)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, normalX, normalY, normalZ);
	}

	public static final class State extends BlockEntityRenderState {
		public float angle;
	}
}
