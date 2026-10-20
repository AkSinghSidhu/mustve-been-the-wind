package com.windsmoke.block;

import com.mojang.serialization.MapCodec;
import com.windsmoke.WindManager;
import com.windsmoke.WindState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A placeable weather vane. The physical arrow is drawn by the block entity
 * renderer, while this block keeps a 16-way rotation state for comparator
 * output. The comparator output is optional and can be toggled by sneak-using
 * the block.
 */
public class WindVaneBlock extends Block implements EntityBlock {
	public static final MapCodec<WindVaneBlock> CODEC = simpleCodec(WindVaneBlock::new);
	public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
	public static final BooleanProperty COMPARATOR = BooleanProperty.create("comparator");
	private static final VoxelShape SHAPE = Shapes.or(
		Block.box(4.0, 0.0, 4.0, 12.0, 2.0, 12.0),
		Block.box(7.0, 2.0, 7.0, 9.0, 12.0, 9.0)
	);

	public WindVaneBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(ROTATION, 0).setValue(COMPARATOR, true));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ROTATION, COMPARATOR);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		if (!state.getValue(COMPARATOR)) {
			return 0;
		}
		// Sixteen wind sectors map exactly onto the comparator's 0-15 range.
		return state.getValue(ROTATION);
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hitResult
	) {
		if (!level.isClientSide()) {
			if (player.isShiftKeyDown()) {
				boolean enabled = !state.getValue(COMPARATOR);
				level.setBlock(pos, state.setValue(COMPARATOR, enabled), 3);
				level.updateNeighbourForOutputSignal(pos, this);
				player.displayClientMessage(
					Component.translatable(enabled
						? "block.windsmoke.wind_vane.comparator_on"
						: "block.windsmoke.wind_vane.comparator_off"),
					true
				);
			} else {
				WindState wind = WindManager.state();
				player.displayClientMessage(
					Component.translatable(
						"block.windsmoke.wind_vane.readout",
						wind.compass(),
						Math.round(wind.strength() * 100.0F),
						Component.translatable(state.getValue(COMPARATOR)
							? "block.windsmoke.wind_vane.comparator_enabled"
							: "block.windsmoke.wind_vane.comparator_disabled")
					),
					true
				);
			}
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WindVaneBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
		Level level,
		BlockState state,
		BlockEntityType<T> blockEntityType
	) {
		if (level.isClientSide() || blockEntityType != ModBlockEntities.WIND_VANE) {
			return null;
		}

		return (tickLevel, tickPos, tickState, blockEntity) ->
			tick(tickLevel, tickPos, tickState);
	}

	private static void tick(Level level, BlockPos pos, BlockState state) {
		// The visual arrow interpolates smoothly on the client. The block state
		// only needs to follow at a modest rate for comparator consumers.
		if (level.getGameTime() % 10L != 0L) {
			return;
		}

		int segment = RotationSegment.convertToSegment(WindManager.state().angle());
		if (state.getValue(ROTATION) != segment) {
			level.setBlock(pos, state.setValue(ROTATION, segment), 3);
			level.updateNeighbourForOutputSignal(pos, ModBlocks.WIND_VANE);
		}
	}
}
