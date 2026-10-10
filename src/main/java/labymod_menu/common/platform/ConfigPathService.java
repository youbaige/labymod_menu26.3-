package labymod_menu.common.platform;

import java.nio.file.Path;

@FunctionalInterface
public interface ConfigPathService {
	Path configDirectory();
}
