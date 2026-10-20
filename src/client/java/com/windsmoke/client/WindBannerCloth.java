package com.windsmoke.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * Emits the banner flag as one continuously deformed cloth instead of a grid of
 * separate boxes. Every quad uses the same simulated particle grid, so
 * neighbouring cells cannot open up or show seams.
 *
 * <p>The top edge is pinned to the banner bar. The cloth is kept slightly in
 * front of the bar/pole and cannot move far enough back to intersect them.
 */
public final class WindBannerCloth {
	/** Marker stored in the banner render-state phase for world banners. */
	public static final float WORLD_PHASE = 10_000_000.0F;

	private static final int COLUMNS = BannerClothSimulation.COLUMNS;
	private static final int ROWS = BannerClothSimulation.ROWS;
	private static final int STRIDE = BannerClothSimulation.STRIDE;
	private static final int POINTS = BannerClothSimulation.POINTS;
	private static final float WIDTH = BannerClothSimulation.WIDTH;
	private static final float HEIGHT = BannerClothSimulation.HEIGHT;
	private static final float FRONT_OFFSET = BannerClothSimulation.FRONT_OFFSET;
	private static final float BACK_OFFSET = -1.6F;
	private static final float PIXEL = 1.0F / 16.0F;
	private static final float FRONT_U = 1.0F;
	private static final float BACK_U = 22.0F;
	private static final float FLAG_V = 1.0F;
	private static final float TEXTURE_SIZE = 64.0F;
	private static int frameCounter;

	private WindBannerCloth() {
	}

	/** Called once per rendered frame by {@code GameRendererMixin}. */
	public static void beginFrame() {
		frameCounter++;
		BannerClothSimulation.beginFrame(frameCounter);
	}

	public static void render(
		PoseStack poseStack,
		VertexConsumer vertexConsumer,
		int light,
		int overlay,
		int color,
		BannerFlagAccess access
	) {
		float baseY = access.windsmoke$baseY();
		float baseZ = access.windsmoke$baseZ();
		int token = Math.round(access.windsmoke$phase() - WORLD_PHASE);
		BannerClothSimulation.Cloth cloth = BannerClothSimulation.acquire(token, baseY, baseZ);

		boolean simulate = BannerIdentity.isNear(token, WindSmokeConfig.effectDistanceBlocks())
			&& BannerClothSimulation.tryActivate(token);
		if (simulate) {
			cloth.advance(localWind(poseStack), WindSmokeConfig.visualStrength());
		} else {
			cloth.rest();
		}
		cloth.prepareGeometry(frameCounter);

		float[] xs = cloth.xs();
		float[] ys = cloth.ys();
		float[] zs = cloth.zs();
		float[] normals = cloth.normals();
		// Distant banners are static and can use a coarser quad grid without
		// changing the cloth's continuous silhouette. Pattern layers are called
		// for each render layer; the positions and normals above are shared.
		int step = simulate ? 1 : 2;

		renderFace(
			vertexConsumer, poseStack, light, overlay, color,
			xs, ys, zs, normals, step, FRONT_U, 0.0F, false
		);
		renderFace(
			vertexConsumer, poseStack, light, overlay, color,
			xs, ys, zs, normals, step, BACK_U, BACK_OFFSET - FRONT_OFFSET, true
		);
	}

	private static Vector3f localWind(PoseStack poseStack) {
		Vector3f local = new Vector3f(
			(float)WindVisuals.windX(),
			0.0F,
			(float)WindVisuals.windZ()
		);
		Matrix3f inverse = new Matrix3f(poseStack.last().pose());
		inverse.invert();
		inverse.transform(local);

		if (local.lengthSquared() < 1.0E-7F) {
			local.set(0.0F, 0.0F, -1.0F);
		} else {
			local.normalize();
		}
		return local;
	}

	private static void renderFace(
		VertexConsumer vertexConsumer,
		PoseStack poseStack,
		int light,
		int overlay,
		int color,
		float[] xs,
		float[] ys,
		float[] zs,
		float[] normals,
		int step,
		float textureX,
		float zOffset,
		boolean back
	) {
		PoseStack.Pose pose = poseStack.last();

		for (int row = 0; row < ROWS; row += step) {
			int nextRow = Math.min(ROWS, row + step);
			float v0 = (FLAG_V + row / (float)ROWS * HEIGHT) / TEXTURE_SIZE;
			float v1 = (FLAG_V + nextRow / (float)ROWS * HEIGHT) / TEXTURE_SIZE;

			for (int column = 0; column < COLUMNS; column += step) {
				int nextColumn = Math.min(COLUMNS, column + step);
				float u0 = (textureX + column / (float)COLUMNS * WIDTH) / TEXTURE_SIZE;
				float u1 = (textureX + nextColumn / (float)COLUMNS * WIDTH) / TEXTURE_SIZE;

				int topLeft = row * STRIDE + column;
				int topRight = row * STRIDE + nextColumn;
				int bottomLeft = nextRow * STRIDE + column;
				int bottomRight = nextRow * STRIDE + nextColumn;

				if (back) {
					// SOUTH face winding, matching vanilla's back face.
					vertex(vertexConsumer, pose, topLeft, xs, ys, zs, normals, zOffset, 1.0F, u0, v0, light, overlay, color);
					vertex(vertexConsumer, pose, topRight, xs, ys, zs, normals, zOffset, 1.0F, u1, v0, light, overlay, color);
					vertex(vertexConsumer, pose, bottomRight, xs, ys, zs, normals, zOffset, 1.0F, u1, v1, light, overlay, color);
					vertex(vertexConsumer, pose, bottomLeft, xs, ys, zs, normals, zOffset, 1.0F, u0, v1, light, overlay, color);
				} else {
					// NORTH face winding, matching vanilla's front face.
					vertex(vertexConsumer, pose, topLeft, xs, ys, zs, normals, zOffset, -1.0F, u0, v0, light, overlay, color);
					vertex(vertexConsumer, pose, bottomLeft, xs, ys, zs, normals, zOffset, -1.0F, u0, v1, light, overlay, color);
					vertex(vertexConsumer, pose, bottomRight, xs, ys, zs, normals, zOffset, -1.0F, u1, v1, light, overlay, color);
					vertex(vertexConsumer, pose, topRight, xs, ys, zs, normals, zOffset, -1.0F, u1, v0, light, overlay, color);
				}
			}
		}
	}

	private static void vertex(
		VertexConsumer vertexConsumer,
		PoseStack.Pose pose,
		int index,
		float[] xs,
		float[] ys,
		float[] zs,
		float[] normals,
		float zOffset,
		float normalSign,
		float u,
		float v,
		int light,
		int overlay,
		int color
	) {
		int normalIndex = index * 3;
		vertexConsumer.addVertex(
			pose,
			xs[index] * PIXEL,
			ys[index] * PIXEL,
			(zs[index] + zOffset) * PIXEL
		);
		// Keep these as separate calls: the banner buffer is wrapped in a
		// SpriteCoordinateExpander, whose addVertex() returns the underlying
		// consumer. Chaining would bypass the sprite-relative UV transform.
		vertexConsumer.setColor(color);
		vertexConsumer.setUv(u, v);
		vertexConsumer.setOverlay(overlay);
		vertexConsumer.setLight(light);
		vertexConsumer.setNormal(
			pose,
			normals[normalIndex] * normalSign,
			normals[normalIndex + 1] * normalSign,
			normals[normalIndex + 2] * normalSign
		);
	}
}
