package com.windsmoke.item;

import com.windsmoke.WindManager;
import com.windsmoke.WindState;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class WindVaneItem extends BlockItem {
	public WindVaneItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		// Right-clicking air still gives a direct reading. Right-clicking a block
		// falls through BlockItem#useOn and places the vane.
		if (!level.isClientSide()) {
			WindState wind = WindManager.state();
			player.sendOverlayMessage(Component.translatable(
				"block.windsmoke.wind_vane.readout_short",
				wind.compass(),
				Math.round(wind.strength() * 100.0F)
			));
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(
		ItemStack itemStack,
		TooltipContext tooltipContext,
		TooltipDisplay tooltipDisplay,
		Consumer<Component> consumer,
		TooltipFlag tooltipFlag
	) {
		consumer.accept(Component.translatable("item.windsmoke.wind_vane.desc").withStyle(ChatFormatting.DARK_GRAY));
	}
}
