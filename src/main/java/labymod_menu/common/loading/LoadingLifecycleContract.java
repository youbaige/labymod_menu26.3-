package labymod_menu.common.loading;

import java.util.Optional;

public interface LoadingLifecycleContract {
	void checkReloadExceptions() throws Throwable;

	void onFinish(Optional<Throwable> failure);

	void initializeCurrentScreen();

	void removeOverlay();
}
