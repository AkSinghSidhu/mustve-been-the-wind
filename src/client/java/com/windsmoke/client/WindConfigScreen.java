package com.windsmoke.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A deliberately small settings screen owned by the mod, rather than a generic
 * generated config page. It is opened from Mod Menu or with the config key
 * (F8 by default).
 */
public final class WindConfigScreen extends Screen {
	private final Screen parent;

	public WindConfigScreen(Screen parent) {
		super(Component.translatable("windsmoke.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int center = this.width / 2;
		int sliderWidth = Math.min(260, Math.max(180, this.width - 80));
		// The smallest supported GUI scale leaves only ~240 logical pixels of
		// height. Compress the stack there so Reset and Done can never overlap.
		boolean compact = this.height < 320;
		int top = compact ? 54 : 64;
		int gap = compact ? 28 : 38;

		this.addRenderableWidget(new IntensitySlider(center - sliderWidth / 2, top, sliderWidth));
		this.addRenderableWidget(new DistanceSlider(center - sliderWidth / 2, top + gap, sliderWidth));
		this.addRenderableWidget(Button.builder(
			qualityLabel(),
			button -> {
				WindSmokeConfig.setVisualQuality((WindSmokeConfig.visualQuality() + 1) % 3);
				button.setMessage(qualityLabel());
			}
		).bounds(center - sliderWidth / 2, top + gap * 2, sliderWidth, 20).build());
		this.addRenderableWidget(new SmokeDensitySlider(center - sliderWidth / 2, top + gap * 3, sliderWidth));
		this.addRenderableWidget(Button.builder(
			Component.translatable("windsmoke.config.reset"),
			button -> {
				WindSmokeConfig.reset();
				this.clearWidgets();
				this.init();
			}
		).bounds(center - 75, top + gap * 4, 150, 20).build());
		boolean canControlWind = this.minecraft != null
			&& this.minecraft.level != null
			&& this.minecraft.getConnection() != null;
		if (canControlWind) {
			int buttonWidth = Math.min(145, (this.width - 30) / 2);
			int buttonGap = 10;
			int buttonStart = center - buttonWidth - buttonGap / 2;
			this.addRenderableWidget(Button.builder(
				Component.translatable("windsmoke.config.debug"),
				button -> this.minecraft.setScreen(new WindDebugScreen(this))
			).bounds(buttonStart, this.height - 30, buttonWidth, 20).build());
			this.addRenderableWidget(Button.builder(
				Component.translatable("windsmoke.config.done"),
				button -> this.onClose()
			).bounds(buttonStart + buttonWidth + buttonGap, this.height - 30, buttonWidth, 20).build());
		} else {
			this.addRenderableWidget(Button.builder(
				Component.translatable("windsmoke.config.done"),
				button -> this.onClose()
			).bounds(center - 75, this.height - 30, 150, 20).build());
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		boolean compact = this.height < 320;
		graphics.drawCenteredString(
			this.font, this.title, this.width / 2, compact ? 10 : 18, 0xFFFFFFFF);
		// These sit above the controls so they can never collide with the
		// bottom Done button on small GUI scales.
		graphics.drawCenteredString(
			this.font,
			Component.translatable("windsmoke.config.note"),
			this.width / 2,
			compact ? 24 : 34,
			0xFFA0A0A0
		);
		graphics.drawCenteredString(
			this.font,
			Component.translatable("windsmoke.config.note.distance"),
			this.width / 2,
			compact ? 36 : 46,
			0xFFA0A0A0
		);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	private static Component qualityLabel() {
		return Component.translatable(
			"windsmoke.config.quality",
			Component.translatable("windsmoke.config.quality." + WindSmokeConfig.visualQuality())
		);
	}

	private static final class IntensitySlider extends AbstractSliderButton {
		private IntensitySlider(int x, int y, int width) {
			super(
				x,
				y,
				width,
				20,
				Component.empty(),
				(WindSmokeConfig.intensity() - WindSmokeConfig.minIntensity())
					/ (WindSmokeConfig.maxIntensity() - WindSmokeConfig.minIntensity())
			);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.translatable(
				"windsmoke.config.intensity",
				Math.round(WindSmokeConfig.intensity() * 100.0F)
			));
		}

		@Override
		protected void applyValue() {
			float raw = WindSmokeConfig.minIntensity()
				+ (float)this.value * (WindSmokeConfig.maxIntensity() - WindSmokeConfig.minIntensity());
			WindSmokeConfig.setIntensity(Math.round(raw * 20.0F) / 20.0F);
		}
	}

	private static final class DistanceSlider extends AbstractSliderButton {
		private DistanceSlider(int x, int y, int width) {
			super(
				x,
				y,
				width,
				20,
				Component.empty(),
				(WindSmokeConfig.simulationDistanceChunks() - WindSmokeConfig.minDistanceChunks())
					/ (float)(WindSmokeConfig.maxDistanceChunks() - WindSmokeConfig.minDistanceChunks())
			);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.translatable(
				"windsmoke.config.distance",
				WindSmokeConfig.simulationDistanceChunks()
			));
		}

		@Override
		protected void applyValue() {
			int chunks = WindSmokeConfig.minDistanceChunks() + Math.round(
				(float)this.value
					* (WindSmokeConfig.maxDistanceChunks() - WindSmokeConfig.minDistanceChunks())
			);
			WindSmokeConfig.setSimulationDistanceChunks(chunks);
		}
	}

	private static final class SmokeDensitySlider extends AbstractSliderButton {
		private SmokeDensitySlider(int x, int y, int width) {
			super(
				x,
				y,
				width,
				20,
				Component.empty(),
				WindSmokeConfig.smokeDensity()
			);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.translatable(
				"windsmoke.config.smoke",
				Math.round(WindSmokeConfig.smokeDensity() * 100.0F)
			));
		}

		@Override
		protected void applyValue() {
			WindSmokeConfig.setSmokeDensity(Math.round((float)this.value * 20.0F) / 20.0F);
		}
	}
}
