package labymod_menu.common.platform;

import java.util.Objects;

public final class ExperienceBootstrap {
	private final LoaderServices services;
	private boolean commonInitializationAttempted;
	private boolean commonInitialized;
	private Throwable commonInitializationFailure;

	public ExperienceBootstrap(LoaderServices services) {
		this.services = Objects.requireNonNull(services, "services");
	}

	public synchronized boolean initializeCommon() {
		if (commonInitialized) {
			return false;
		}
		if (commonInitializationFailure != null) {
			rethrowUnchecked(commonInitializationFailure);
		}
		if (commonInitializationAttempted) {
			throw new IllegalStateException("Common initialization is already in progress");
		}
		commonInitializationAttempted = true;
		try {
			services.soundRegistration().registerSounds();
			commonInitialized = true;
		} catch (Throwable throwable) {
			commonInitializationFailure = throwable;
			rethrowUnchecked(throwable);
		}
		return true;
	}

	public synchronized boolean isCommonInitialized() {
		return commonInitialized;
	}

	public LoaderServices services() {
		return services;
	}

	private static void rethrowUnchecked(Throwable throwable) {
		if (throwable instanceof RuntimeException) {
			throw (RuntimeException) throwable;
		}
		if (throwable instanceof Error) {
			throw (Error) throwable;
		}
		throw new IllegalStateException("Loader initialization threw a checked exception", throwable);
	}
}
