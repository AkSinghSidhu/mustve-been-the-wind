package com.windsmoke.client;

import com.windsmoke.net.WindControlPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Operator-facing controls for the server-authoritative wind. This exists so
 * testing does not require typing /wind commands. The button is opened from
 * the normal wind settings screen and the server validates permission again
 * when the packet arrives.
 */
public final class WindDebugScreen extends Screen {
	private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	private final Screen parent;
	private int directionDegrees;
	private int strengthPercent;

	public WindDebugScreen(Screen parent) {
		super(Component.translatable("windsmoke.debug.title"));
		this.parent = parent;
		this.directionDegrees = Math.round(Mth.wrapDegrees(WindClientState.angle()) + 360.0F) % 360;
		this.strengthPercent = Math.round(WindClientState.strength() * 100.0F);
	}

	@Override
	protected void init() {
		int center = this.width / 2;
		int controlWidth = Math.min(240, Math.max(180, this.width - 80));
		boolean compact = this.height < 320;
		int top = compact ? 54 : 64;
		int gap = compact ? 34 : 40;

		this.addRenderableWidget(new DirectionSlider(
			center - controlWidth / 2, top, controlWidth, this.directionDegrees));
		this.addRenderableWidget(new StrengthSlider(
			center - controlWidth / 2, top + gap, controlWidth, this.strengthPercent));
		this.addRenderableWidget(Button.builder(
			Component.translatable("windsmoke.debug.apply"),
			button -> this.apply()
		).bounds(center - 100, top + gap * 2, 200, 20).build());
		this.addRenderableWidget(Button.builder(
			Component.translatable("windsmoke.debug.release"),
			button -> {
				ClientPlayNetworking.send(new WindControlPayload(
					this.directionDegrees, this.strengthPercent / 100.0F, true));
			}
		).bounds(center - 100, top + gap * 2 + 24, 200, 20).build());
		this.addRenderableWidget(Button.builder(
			Component.translatable("windsmoke.debug.back"),
			button -> this.onClose()
		).bounds(center - 75, this.height - 30, 150, 20).build());
	}

	private void apply() {
		ClientPlayNetworking.send(new WindControlPayload(
			this.directionDegrees, this.strengthPercent / 100.0F, false));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		boolean compact = this.height < 320;
		graphics.drawCenteredString(
			this.font, this.title, this.width / 2, compact ? 10 : 18, 0xFFFFFFFF);
		graphics.drawCenteredString(
			this.font,
			Component.translatable("windsmoke.debug.note"),
			this.width / 2,
			compact ? 24 : 34,
			0xFFA0A0A0
		);
		graphics.drawCenteredString(
			this.font,
			Component.translatable(
				"windsmoke.debug.current",
				compass(WindClientState.angle()),
				Math.round(WindClientState.strength() * 100.0F)
			),
			this.width / 2,
			compact ? this.height - 56 : this.height - 64,
			0xFFFFD27F
		);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	private static String compass(float angle) {
		return COMPASS[Math.floorMod(Math.round(angle / 45.0F), 8)];
	}

	private final class DirectionSlider extends AbstractSliderButton {
		private DirectionSlider(int x, int y, int width, int degrees) {
			super(x, y, width, 20, Component.empty(), degrees / 360.0);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int degrees = Math.round((float)this.value * 360.0F) % 360;
			this.setMessage(Component.translatable(
				"windsmoke.debug.direction", degrees, compass(degrees)));
		}

		@Override
		protected void applyValue() {
			directionDegrees = Math.round((float)this.value * 360.0F) % 360;
		}
	}

	private final class StrengthSlider extends AbstractSliderButton {
		private StrengthSlider(int x, int y, int width, int percent) {
			super(x, y, width, 20, Component.empty(), percent / 100.0);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.translatable(
				"windsmoke.debug.strength", Math.round((float)this.value * 100.0F)));
		}

		@Override
		protected void applyValue() {
			strengthPercent = Math.round((float)this.value * 100.0F);
		}
	}
}
