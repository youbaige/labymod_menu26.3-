package labymod_menu.common.platform;

import java.util.Objects;

public final class LoaderServices {
	private final ConfigPathService configPath;
	private final ModPresenceService modPresence;
	private final SoundRegistrationService soundRegistration;

	public LoaderServices(ConfigPathService configPath, ModPresenceService modPresence,
			SoundRegistrationService soundRegistration) {
		this.configPath = Objects.requireNonNull(configPath, "configPath");
		this.modPresence = Objects.requireNonNull(modPresence, "modPresence");
		this.soundRegistration = Objects.requireNonNull(soundRegistration, "soundRegistration");
	}

	public ConfigPathService configPath() {
		return configPath;
	}

	public ModPresenceService modPresence() {
		return modPresence;
	}

	public SoundRegistrationService soundRegistration() {
		return soundRegistration;
	}
}
