package labymod_menu.common.config;

public final class ConfigValues {
	private final boolean mojangLogoAnimation;
	private final boolean darkLoadingScreen;
	private final boolean minecraftTitleAnimation;

	public ConfigValues(boolean mojangLogoAnimation, boolean darkLoadingScreen,
			boolean minecraftTitleAnimation) {
		this.mojangLogoAnimation = mojangLogoAnimation;
		this.darkLoadingScreen = darkLoadingScreen;
		this.minecraftTitleAnimation = minecraftTitleAnimation;
	}

	public static ConfigValues defaults() {
		return new ConfigValues(true, false, true);
	}

	public boolean isMojangLogoAnimationEnabled() {
		return mojangLogoAnimation;
	}

	public boolean isDarkLoadingScreenEnabled() {
		return darkLoadingScreen;
	}

	public boolean isMinecraftTitleAnimationEnabled() {
		return minecraftTitleAnimation;
	}

	public ConfigValues withMojangLogoAnimation(boolean enabled) {
		return new ConfigValues(enabled, darkLoadingScreen, minecraftTitleAnimation);
	}

	public ConfigValues withDarkLoadingScreen(boolean enabled) {
		return new ConfigValues(mojangLogoAnimation, enabled, minecraftTitleAnimation);
	}

	public ConfigValues withMinecraftTitleAnimation(boolean enabled) {
		return new ConfigValues(mojangLogoAnimation, darkLoadingScreen, enabled);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ConfigValues)) {
			return false;
		}
		ConfigValues values = (ConfigValues) other;
		return mojangLogoAnimation == values.mojangLogoAnimation
				&& darkLoadingScreen == values.darkLoadingScreen
				&& minecraftTitleAnimation == values.minecraftTitleAnimation;
	}

	@Override
	public int hashCode() {
		int result = mojangLogoAnimation ? 1 : 0;
		result = 31 * result + (darkLoadingScreen ? 1 : 0);
		result = 31 * result + (minecraftTitleAnimation ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ConfigValues[mojangLogoAnimation=" + mojangLogoAnimation
				+ ", darkLoadingScreen=" + darkLoadingScreen
				+ ", minecraftTitleAnimation=" + minecraftTitleAnimation + "]";
	}
}
