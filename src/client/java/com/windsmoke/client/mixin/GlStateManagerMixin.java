package com.windsmoke.client.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.windsmoke.client.WindShaderPatcher;
import com.windsmoke.client.WindShaderUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlStateManager.class, remap = false)
public abstract class GlStateManagerMixin {
	@ModifyVariable(method = "glShaderSource", at = @At("HEAD"), argsOnly = true, remap = false)
	private static String windsmoke$patchShader(String source) {
		return WindShaderPatcher.patch(source);
	}

	@Inject(method = "_glUseProgram", at = @At("TAIL"), remap = false)
	private static void windsmoke$bindWindUniforms(int program, CallbackInfo ci) {
		WindShaderUniforms.onUse(program);
	}
}
