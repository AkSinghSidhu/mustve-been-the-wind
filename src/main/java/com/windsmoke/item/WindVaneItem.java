package com.windsmoke.item;

import com.windsmoke.WindManager;
import com.windsmoke.WindState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class WindVaneItem extends Item {
	public WindVaneItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide()) {
			WindState wind = WindManager.state();
			player.displayClientMessage(Component.literal(String.format(
				"Wind blows %s at %.0f%% strength", wind.compass(), wind.strength() * 100.0F)), true);
		}

		return InteractionResult.SUCCESS;
	}
}
