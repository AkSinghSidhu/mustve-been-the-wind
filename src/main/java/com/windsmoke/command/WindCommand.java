package com.windsmoke.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.windsmoke.WindClimate;
import com.windsmoke.WindEvent;
import com.windsmoke.WindManager;
import com.windsmoke.WindSmoke;
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
							FloatArgumentType.getFloat(command, "strength"))))))
			.then(Commands.literal("auto")
				.executes(command -> release(command.getSource())))
			.then(Commands.literal("event")
				.executes(command -> listEvents(command.getSource()))
				.then(Commands.argument("type", StringArgumentType.word())
					.suggests((commandContext, builder) -> {
						for (WindEvent event : WindEvent.values()) {
							builder.suggest(event.id());
						}

						return builder.buildFuture();
					})
					.executes(command -> trigger(command.getSource(),
						StringArgumentType.getString(command, "type"))))));
	}

	private static int report(CommandSourceStack source) {
		WindState wind = WindManager.state();
		WindClimate.Range range = wind.currentRange(source.getServer());
		String event = wind.eventLabel();
		String suffix = wind.pinned() ? ", pinned" : event != null ? ", " + event : "";
		String text = String.format("Wind blows %s (%.0f deg) at %.0f%% strength [%s %.0f-%.0f%%%s]",
			wind.compass(), wind.angle(), wind.strength() * 100.0F, wind.weatherLabel(source.getServer()),
			range.min() * 100.0F, range.max() * 100.0F, suffix);
		if (Boolean.getBoolean("windsmoke.testharness")) {
			WindSmoke.LOGGER.info("[windsmoke] {}", text);
		}
		source.sendSuccess(() -> Component.literal(text), false);
		return 1;
	}

	private static int listEvents(CommandSourceStack source) {
		StringBuilder names = new StringBuilder();

		for (WindEvent event : WindEvent.values()) {
			if (!names.isEmpty()) {
				names.append(", ");
			}

			names.append(event.id());
		}

		source.sendSuccess(() -> Component.literal("Wind events: " + names), false);
		return 1;
	}

	private static int trigger(CommandSourceStack source, String name) {
		WindEvent event = WindEvent.byId(name);

		if (event == null) {
			source.sendFailure(Component.literal("Unknown wind event: " + name));
			return 0;
		}

		boolean wasPinned = WindManager.state().pinned();
		WindManager.state().forceEvent(event, source.getServer().overworld());
		WindManager.syncAll(source.getServer());
		source.sendSuccess(() -> Component.literal(
			(wasPinned ? "Wind released; triggered " : "Triggered ") + event.displayName()), true);
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

	private static int release(CommandSourceStack source) {
		WindManager.state().unpin();
		source.sendSuccess(() -> Component.literal("Wind released to natural drift"), true);
		return 1;
	}
}
