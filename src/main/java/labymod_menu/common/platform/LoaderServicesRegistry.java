package labymod_menu.common.platform;

import java.util.Objects;

public final class LoaderServicesRegistry {
	private LoaderServices services;

	public synchronized boolean install(LoaderServices candidate) {
		Objects.requireNonNull(candidate, "candidate");
		if (services == null) {
			services = candidate;
			return true;
		}
		if (services == candidate) {
			return false;
		}
		throw new IllegalStateException("Loader services are already installed");
	}

	public synchronized LoaderServices requireServices() {
		if (services == null) {
			throw new IllegalStateException("Loader services have not been installed");
		}
		return services;
	}

	public synchronized boolean isInstalled() {
		return services != null;
	}
}
