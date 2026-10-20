package com.windsmoke.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/**
 * Optional Mod Menu hook. The screen itself is ours so Mod Menu is not a hard
 * dependency; without Mod Menu the same screen is available on F8.
 */
public final class WindModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return (ConfigScreenFactory<Screen>)WindConfigScreen::new;
	}
}
