package com.windsmoke.client.mixin.sodium;

import com.windsmoke.client.WindShaderUniforms;
import net.caffeinemc.mods.sodium.client.gl.GlObject;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium binds its own GL programs without going through
 * {@code GlStateManager._glUseProgram}. Upload the wind uniforms whenever one
 * of those programs becomes current; non-wind programs simply have no matching
 * uniform locations and are ignored.
 */
@Mixin(value = GlProgram.class, remap = false)
public abstract class SodiumGlProgramMixin {
	@Inject(method = "bind", at = @At("TAIL"))
	private void windsmoke$uploadUniforms(CallbackInfo ci) {
		WindShaderUniforms.onUse(((GlObject)(Object)this).handle());
	}
}
