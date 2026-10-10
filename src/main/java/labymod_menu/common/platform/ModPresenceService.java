package labymod_menu.common.platform;

@FunctionalInterface
public interface ModPresenceService {
	boolean isModLoaded(String modId);
}
