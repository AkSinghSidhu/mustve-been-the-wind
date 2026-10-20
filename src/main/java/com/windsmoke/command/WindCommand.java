package com.windsmoke.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.windsmoke.WindManager;
import com.windsmoke.WindState;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class WindCommand {
	private WindCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
			Commands.CommandSelection selection) {
		dispatcher.register(Commands.literal("wind")
			.executes(command -> report(command.getSource()))
			.then(Commands.literal("set")
				.then(Commands.argument("degrees", FloatArgumentType.floatArg(0.0F, 360.0F))
					.then(Commands.argument("strength", FloatArgumentType.floatArg(0.0F, 1.0F))
						.executes(command -> set(command.getSource(),
							FloatArgumentType.getFloat(command, "degrees"),
							FloatArgumentType.getFloat(command, "strength")))))));
	}

	private static int report(CommandSourceStack source) {
		WindState wind = WindManager.state();
		source.sendSuccess(() -> Component.literal(String.format(
			"Wind blows %s (%.0f deg) at %.0f%% strength",
			wind.compass(), wind.angle(), wind.strength() * 100.0F)), false);
		return 1;
	}

	private static int set(CommandSourceStack source, float degrees, float strength) {
		WindManager.state().set(degrees, strength);
		WindManager.syncAll(source.getServer());
		source.sendSuccess(() -> Component.literal(String.format(
			"Wind pinned to %s (%.0f deg) at %.0f%% strength",
			WindManager.state().compass(), degrees, strength * 100.0F)), true);
		return 1;
	}
}
