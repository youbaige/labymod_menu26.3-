package labymod_menu.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class VersionScreens {
	private VersionScreens() {}

	static void open(Minecraft minecraft, Screen screen) {
		minecraft.gui.setScreen(screen);
	}
}
