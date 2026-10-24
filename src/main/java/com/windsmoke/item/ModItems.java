package com.windsmoke.item;

import com.windsmoke.WindSmoke;
import com.windsmoke.block.ModBlocks;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static final ResourceKey<Item> WIND_VANE_KEY =
		ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(WindSmoke.MOD_ID, "wind_vane"));

	public static final WindVaneItem WIND_VANE =
		new WindVaneItem(ModBlocks.WIND_VANE, new Item.Properties().setId(WIND_VANE_KEY));

	private ModItems() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, WIND_VANE_KEY, WIND_VANE);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS)
			.register(output -> output.accept(WIND_VANE));
	}
}
