package labymod_menu.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;

public final class PropertiesConfigStore {
	public static final String MOJANG_LOGO_ANIMATION = "mojangLogoAnimation";
	public static final String DARK_LOADING_SCREEN = "darkLoadingScreen";
	public static final String MINECRAFT_TITLE_ANIMATION = "minecraftTitleAnimation";

	private final Path file;

	public PropertiesConfigStore(Path file) {
		this.file = Objects.requireNonNull(file, "file");
	}

	public Path file() {
		return file;
	}

	public ConfigValues load() {
		ConfigValues defaults = ConfigValues.defaults();
		if (!Files.isRegularFile(file)) {
			return defaults;
		}
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(file)) {
			properties.load(input);
		} catch (IOException | IllegalArgumentException ignored) {
			return defaults;
		}
		return new ConfigValues(
				booleanProperty(properties, MOJANG_LOGO_ANIMATION,
						defaults.isMojangLogoAnimationEnabled()),
				booleanProperty(properties, DARK_LOADING_SCREEN,
						defaults.isDarkLoadingScreenEnabled()),
				booleanProperty(properties, MINECRAFT_TITLE_ANIMATION,
						defaults.isMinecraftTitleAnimationEnabled()));
	}

	public boolean save(ConfigValues values) {
		Objects.requireNonNull(values, "values");
		Properties properties = new Properties();
		properties.setProperty(MOJANG_LOGO_ANIMATION,
				Boolean.toString(values.isMojangLogoAnimationEnabled()));
		properties.setProperty(DARK_LOADING_SCREEN,
				Boolean.toString(values.isDarkLoadingScreenEnabled()));
		properties.setProperty(MINECRAFT_TITLE_ANIMATION,
				Boolean.toString(values.isMinecraftTitleAnimationEnabled()));
		Path target = file.toAbsolutePath();
		Path parent = target.getParent();
		Path temporary = null;
		try {
			Files.createDirectories(parent);
			temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
			try (OutputStream output = Files.newOutputStream(temporary)) {
				properties.store(output, "LabyMod Menu settings");
			}
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
			temporary = null;
			return true;
		} catch (IOException ignored) {
			return false;
		} finally {
			if (temporary != null) {
				try {
					Files.deleteIfExists(temporary);
				} catch (IOException ignored) {
				}
			}
		}
	}

	private static boolean booleanProperty(Properties properties, String key, boolean fallback) {
		String value = properties.getProperty(key);
		if ("true".equalsIgnoreCase(value)) {
			return true;
		}
		if ("false".equalsIgnoreCase(value)) {
			return false;
		}
		return fallback;
	}
}
