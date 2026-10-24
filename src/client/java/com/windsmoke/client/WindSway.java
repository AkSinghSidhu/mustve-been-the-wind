package com.windsmoke.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.windsmoke.WindSmoke;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Tags wind-sensitive vertices during chunk construction.
 *
 * <p>The actual movement happens on the GPU. Vertices belonging to swaying
 * blocks get a small code stored in their colour alpha; the patched terrain
 * shader decodes it and applies a per-frame offset. This avoids rebuilding
 * chunk meshes as the wind changes.
 */
public final class WindSway {
	public static final TagKey<Block> WIND_SENSITIVE = TagKey.create(
		Registries.BLOCK, Identifier.fromNamespaceAndPath(WindSmoke.MOD_ID, "wind_sensitive"));
	public static final TagKey<Block> WIND_HANGING = TagKey.create(
		Registries.BLOCK, Identifier.fromNamespaceAndPath(WindSmoke.MOD_ID, "wind_hanging"));

	/** Shader code categories. Each category owns a 16-value weight range. */
	static final int CATEGORY_PLANT = 0;
	static final int CATEGORY_HANGING = 1;
	static final int CATEGORY_CROP = 2;
	static final int CATEGORY_LEAF = 3;

	/**
	 * Sodium uses a different chunk renderer and shader. Its vertices are
	 * tagged by the Sodium compatibility mixins instead of this consumer
	 * wrapper, so the wrapper stays disabled under Sodium.
	 */
	private static final boolean SODIUM = FabricLoader.getInstance().isModLoaded("sodium");

	private static final ThreadLocal<WindVertexConsumer> WRAPPERS =
		ThreadLocal.withInitial(WindVertexConsumer::new);
	private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

	private WindSway() {
	}

	public static void begin(BlockState state, BlockPos pos) {
		Profile profile = profile(state);

		if (profile == Profile.NONE) {
			CONTEXT.remove();
			return;
		}

		CONTEXT.set(new Context(state, pos, profile));
	}

	public static void end() {
		CONTEXT.remove();
	}

	/** Used by Fabric's Indigo renderer, which bypasses renderBatched. */
	public static VertexConsumer wrapCurrent(VertexConsumer delegate) {
		Context context = CONTEXT.get();
		return context == null ? delegate : wrap(context.state(), context.pos(), delegate);
	}

	/**
	 * Tags a vertex emitted by Sodium's chunk renderer. Sodium writes vertices
	 * directly instead of using a {@link VertexConsumer}, so the compatibility
	 * mixins call this with the block currently being rendered.
	 */
	public static int tagCurrent(int color, float vertexY) {
		Context context = CONTEXT.get();
		return context == null ? color : tagColor(context.state(), context.pos(), context.profile(), color, vertexY);
	}

	public static VertexConsumer wrap(BlockState state, BlockPos pos, VertexConsumer delegate) {
		if (SODIUM) {
			return delegate;
		}

		Profile profile = profile(state);

		if (profile == Profile.NONE) {
			return delegate;
		}

		WindVertexConsumer wrapper = WRAPPERS.get();
		wrapper.set(delegate, state, pos, profile);

		return wrapper;
	}

	private static Profile profile(BlockState state) {
		if (state.is(WIND_HANGING)) {
			if (state.is(BlockTags.LANTERNS)
				&& state.hasProperty(BlockStateProperties.HANGING)
				&& !state.getValue(BlockStateProperties.HANGING)) {
				return Profile.NONE;
			}
			return Profile.HANGING;
		}

		if (state.is(BlockTags.CROPS) || state.is(Blocks.NETHER_WART)) {
			return Profile.CROP;
		}

		if (state.is(BlockTags.LEAVES)) {
			return Profile.LEAF;
		}

		return state.is(WIND_SENSITIVE) ? Profile.PLANT : Profile.NONE;
	}

	private enum Profile {
		NONE,
		PLANT,
		CROP,
		LEAF,
		HANGING
	}

	private record Context(BlockState state, BlockPos pos, Profile profile) {
	}

	private static int tagColor(BlockState state, BlockPos pos, Profile profile, int color, float vertexY) {
		// Chunk meshes are stored section-local, so undo the block's
		// section-relative origin rather than its absolute world Y.
		float localY = vertexY - (pos.getY() & 15);
		int category;

		switch (profile) {
			case HANGING -> {
				float weight = 1.0F - Mth.clamp(localY, 0.0F, 1.0F);
				return withCode(color, CATEGORY_HANGING, weight);
			}
			case CROP -> category = CATEGORY_CROP;
			case LEAF -> category = CATEGORY_LEAF;
			default -> category = CATEGORY_PLANT;
		}

		float weight;
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
				// Continue the lower half's lean: 0.5 at the seam, 1.0 at the top.
				weight = 0.5F + 0.5F * Mth.clamp(localY, 0.0F, 1.0F);
			} else {
				// Stop at 0.5 where the upper half begins.
				weight = 0.5F * Mth.clamp(localY, 0.0F, 1.0F);
			}
		} else {
			weight = Mth.clamp(localY, 0.0F, 1.0F);
		}

		return withCode(color, category, weight);
	}

	private static int withCode(int color, int category, float weight) {
		int code = category * 16 + Math.round(Mth.clamp(weight, 0.0F, 1.0F) * 15.0F);
		int alpha = 254 - code;
		return (color & 0xFFFFFF) | (alpha << 24);
	}

	private static final class WindVertexConsumer implements VertexConsumer {
		private VertexConsumer delegate;
		private BlockState state;
		private BlockPos pos;
		private Profile profile;
		private float currentY;

		private void set(VertexConsumer delegate, BlockState state, BlockPos pos, Profile profile) {
			this.delegate = delegate;
			this.state = state;
			this.pos = pos;
			this.profile = profile;
		}

		private int tag(int color, float vertexY) {
			return tagColor(this.state, this.pos, this.profile, color, vertexY);
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			this.currentY = y;
			return this.delegate.addVertex(x, y, z);
		}

		@Override
		public void addVertex(
			float x, float y, float z, int color, float u, float v, int overlay, int light,
			float normalX, float normalY, float normalZ
		) {
			int tagged = this.tag(color, y);

			this.delegate.addVertex(
				x, y, z, tagged, u, v, overlay, light,
				normalX, normalY, normalZ
			);
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			int color = (alpha & 0xFF) << 24
				| (red & 0xFF) << 16
				| (green & 0xFF) << 8
				| (blue & 0xFF);
			return this.delegate.setColor(this.tag(color, this.currentY));
		}

		@Override
		public VertexConsumer setColor(int color) {
			return this.delegate.setColor(this.tag(color, this.currentY));
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			return this.delegate.setUv(u, v);
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this.delegate.setUv1(u, v);
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this.delegate.setUv2(u, v);
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return this.delegate.setNormal(x, y, z);
		}

		@Override
		public VertexConsumer setLineWidth(float width) {
			return this.delegate.setLineWidth(width);
		}
	}
}
