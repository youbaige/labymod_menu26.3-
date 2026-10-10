package labymod_menu.common.loading;

import java.util.Objects;
import java.util.Optional;

public final class LoadingLifecycleState {
	private boolean reloadCompletionDispatched;
	private boolean reloadCompletionFinished;
	private boolean screenInitializationDispatched;
	private boolean overlayExitStarted;
	private boolean overlayRemoved;
	private long overlayExitStartedAtMillis = -1L;

	public boolean tryCompleteReload(boolean reloadDone, boolean fadeInReady,
			LoadingLifecycleContract contract) {
		Objects.requireNonNull(contract, "contract");
		if (reloadCompletionDispatched || !reloadDone || !fadeInReady) {
			return false;
		}

		reloadCompletionDispatched = true;
		Throwable reloadFailure = null;
		try {
			contract.checkReloadExceptions();
		} catch (Throwable throwable) {
			reloadFailure = throwable;
		}

		Throwable completionFailure = null;
		try {
			contract.onFinish(Optional.ofNullable(reloadFailure));
		} catch (Throwable throwable) {
			completionFailure = throwable;
		}

		Throwable initializationFailure = null;
		if (!screenInitializationDispatched) {
			screenInitializationDispatched = true;
			try {
				contract.initializeCurrentScreen();
			} catch (Throwable throwable) {
				initializationFailure = throwable;
			} finally {
				reloadCompletionFinished = true;
			}
		}

		if (completionFailure != null) {
			if (initializationFailure != null && initializationFailure != completionFailure) {
				completionFailure.addSuppressed(initializationFailure);
			}
			rethrowUnchecked(completionFailure);
		}
		if (initializationFailure != null) {
			rethrowUnchecked(initializationFailure);
		}
		return true;
	}

	public boolean tryStartOverlayExit(boolean visualExitReady, long nowMillis) {
		if (nowMillis < 0L) {
			throw new IllegalArgumentException("nowMillis must not be negative");
		}
		if (!reloadCompletionFinished || !visualExitReady || overlayExitStarted) {
			return false;
		}
		overlayExitStarted = true;
		overlayExitStartedAtMillis = nowMillis;
		return true;
	}

	public boolean tryRemoveOverlay(long nowMillis, long settleMillis, LoadingLifecycleContract contract) {
		Objects.requireNonNull(contract, "contract");
		if (nowMillis < 0L) {
			throw new IllegalArgumentException("nowMillis must not be negative");
		}
		if (settleMillis < 0L) {
			throw new IllegalArgumentException("settleMillis must not be negative");
		}
		if (!overlayExitStarted || overlayRemoved || nowMillis < overlayExitStartedAtMillis
				|| nowMillis - overlayExitStartedAtMillis < settleMillis) {
			return false;
		}
		overlayRemoved = true;
		contract.removeOverlay();
		return true;
	}

	public boolean isReloadCompletionDispatched() {
		return reloadCompletionDispatched;
	}

	public boolean isOverlayExitStarted() {
		return overlayExitStarted;
	}

	public boolean isOverlayRemoved() {
		return overlayRemoved;
	}

	public long getOverlayExitStartedAtMillis() {
		return overlayExitStartedAtMillis;
	}

	private static void rethrowUnchecked(Throwable throwable) {
		if (throwable instanceof RuntimeException) {
			throw (RuntimeException) throwable;
		}
		if (throwable instanceof Error) {
			throw (Error) throwable;
		}
		throw new IllegalStateException("Loading lifecycle callback threw a checked exception", throwable);
	}
}
