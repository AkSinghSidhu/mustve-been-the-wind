package com.windsmoke.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WindVaneBlockEntity extends BlockEntity {
	public WindVaneBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WIND_VANE, pos, state);
	}
}
