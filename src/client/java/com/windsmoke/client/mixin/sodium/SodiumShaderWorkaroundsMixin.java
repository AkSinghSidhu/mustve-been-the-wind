package com.windsmoke.client.mixin.sodium;

import com.windsmoke.client.WindShaderPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Sodium passes its fully expanded shader source to this package-private
 * helper. Patch it there because Sodium compiles through LWJGL directly rather
 * than Minecraft's {@code GlStateManager} shader path.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderWorkarounds", remap = false)
public abstract class SodiumShaderWorkaroundsMixin {
	@ModifyVariable(method = "safeShaderSource", at = @At("HEAD"), argsOnly = true)
	private static CharSequence windsmoke$patchShader(CharSequence source) {
		return WindShaderPatcher.patch(source == null ? null : source.toString());
	}
}
