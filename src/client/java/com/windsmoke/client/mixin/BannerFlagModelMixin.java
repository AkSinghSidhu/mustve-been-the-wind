package com.windsmoke.client.mixin;

import com.windsmoke.client.BannerFlagAccess;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records the banner's world phase and attachment pivot. The actual cloth is
 * rendered as one continuous deformed surface by {@code WindBannerCloth}; the
 * vanilla model remains untouched for inventory and item previews.
 */
@Mixin(BannerFlagModel.class)
public abstract class BannerFlagModelMixin implements BannerFlagAccess {
	@Shadow
	@Final
	private ModelPart flag;

	@Unique
	private float windsmoke$phase;

	@Unique
	private float windsmoke$baseY;

	@Unique
	private float windsmoke$baseZ;

	@Inject(method = "setupAnim", at = @At("TAIL"))
	private void windsmoke$capturePose(Float phase, CallbackInfo ci) {
		this.windsmoke$phase = phase;
		this.windsmoke$baseY = this.flag.y;
		this.windsmoke$baseZ = this.flag.z;
	}

	@Override
	public float windsmoke$phase() {
		return this.windsmoke$phase;
	}

	@Override
	public float windsmoke$baseY() {
		return this.windsmoke$baseY;
	}

	@Override
	public float windsmoke$baseZ() {
		return this.windsmoke$baseZ;
	}
}
