package com.windsmoke.block;

import com.windsmoke.WindSmoke;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final ResourceKey<BlockEntityType<?>> WIND_VANE_KEY = ResourceKey.create(
		Registries.BLOCK_ENTITY_TYPE,
		Identifier.fromNamespaceAndPath(WindSmoke.MOD_ID, "wind_vane")
	);

	public static final BlockEntityType<WindVaneBlockEntity> WIND_VANE = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		WIND_VANE_KEY,
		FabricBlockEntityTypeBuilder.create(WindVaneBlockEntity::new, ModBlocks.WIND_VANE).build()
	);

	private ModBlockEntities() {
	}

	public static void register() {
		// Static field registration is enough; this call documents and forces
		// class initialization from the common initializer.
	}
}
