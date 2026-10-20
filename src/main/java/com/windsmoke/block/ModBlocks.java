package com.windsmoke.block;

import com.windsmoke.WindSmoke;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
	public static final ResourceKey<Block> WIND_VANE_KEY = ResourceKey.create(
		Registries.BLOCK,
		Identifier.fromNamespaceAndPath(WindSmoke.MOD_ID, "wind_vane")
	);

	public static final WindVaneBlock WIND_VANE = new WindVaneBlock(
		BlockBehaviour.Properties.of()
			.setId(WIND_VANE_KEY)
			.mapColor(MapColor.METAL)
			.strength(1.5F)
			.sound(SoundType.COPPER)
			.noOcclusion()
	);

	private ModBlocks() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK, WIND_VANE_KEY, WIND_VANE);
	}
}
