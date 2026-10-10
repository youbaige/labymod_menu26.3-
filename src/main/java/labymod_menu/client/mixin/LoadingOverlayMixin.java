package labymod_menu.client.mixin;

import labymod_menu.client.internal.MojangAnimation;
import labymod_menu.client.internal.TitleOpeningController;
import labymod_menu.common.loading.LoadingLifecycleContract;
import labymod_menu.common.loading.LoadingLifecycleState;
import labymod_menu.config.LabyModMenuConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.GenericWaitingScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.IntSupplier;

@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {
	@Mutable
	@Shadow
	@Final
	private static IntSupplier BRAND_BACKGROUND;
	@Unique
	private static final int LIGHT_BACKGROUND = 0xFFEF323D;
	@Unique
	private static final int DARK_BACKGROUND = 0xFF000000;
	@Unique
	private static final int CUSTOM_DARK_BACKGROUND = 0xFF14181C;
	@Unique
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	@Unique
	private static final long MENU_COVER_SETTLE_MILLIS = 120L;
	@Unique
	private static final Identifier VANILLA_LOGO = Identifier.withDefaultNamespace("textures/gui/title/mojangstudios.png");
	@Unique
	private final LoadingLifecycleState labyModMenu$lifecycle = new LoadingLifecycleState();

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void labyModMenu$replaceLoadingBackground(CallbackInfo ci) {
		IntSupplier vanillaBackground = BRAND_BACKGROUND;
		BRAND_BACKGROUND = () -> LabyModMenuConfig.isDarkLoadingScreenEnabled()
				? CUSTOM_DARK_BACKGROUND : vanillaBackground.getAsInt();
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void labyModMenu$render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		LoadingOverlayAccessor overlay = (LoadingOverlayAccessor) this;
		if (!LabyModMenuConfig.isMojangLogoAnimationEnabled() || overlay.labyModMenu$getFadeIn()) {
			return;
		}
		ci.cancel();
		long now = Util.getMillis();
		boolean serverTransition = isServerTransition(overlay);

		tickFadeIn(overlay, now);
		smoothProgress(overlay);

		if (!serverTransition && shouldStartAnimation(overlay)) {
			MojangAnimation.start();
		}
		if (!serverTransition) {
			holdFadeOutUntilAnimationFinishes(overlay);
		}

		float fadeOut = fadeOutProgress(overlay, now);
		float fadeIn = fadeInProgress(overlay, now);
		int background = backgroundColor(overlay);
		if (serverTransition) {
			TitleOpeningController.renderBackground(graphics);
		} else {
			graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), background);
			if (MojangAnimation.hasStarted()) {
				MojangAnimation.render(graphics);
			} else {
				renderVanillaLogo(graphics);
			}
			renderProgressBar(graphics, overlay, fadeOut, background);
		}

		boolean fadeInReady = !overlay.labyModMenu$getFadeIn() || fadeIn >= 2.0F;
		if (canFinishReload(overlay, fadeInReady)) {
			finishReload(overlay, graphics, fadeInReady);
		}

		if (labyModMenu$lifecycle.tryStartOverlayExit(
				serverTransition || MojangAnimation.isFinished(), now)) {
			overlay.labyModMenu$setFadeOutStart(now);
			TitleOpeningController.playOpeningForLoadingTransition();
		}

		if (labyModMenu$lifecycle.isOverlayExitStarted()
				&& !labyModMenu$lifecycle.isOverlayRemoved()) {
			TitleOpeningController.renderHiddenTitleOverlay(graphics);
		}

		if (labyModMenu$lifecycle.isOverlayExitStarted()
				&& !labyModMenu$lifecycle.isOverlayRemoved()
				&& labyModMenu$lifecycle.tryRemoveOverlay(
						now, MENU_COVER_SETTLE_MILLIS, lifecycleContract(overlay, graphics))) {
			TitleOpeningController.playOpening();
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void labyModMenu$startTitleAfterVanillaLoading(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float delta, CallbackInfo ci) {
		LoadingOverlayAccessor overlay = (LoadingOverlayAccessor) this;
		//? if <26.3 {
		/*if (!overlay.labyModMenu$getFadeIn() && !LabyModMenuConfig.isMojangLogoAnimationEnabled()
				&& overlay.labyModMenu$getMinecraft().getOverlay() == null) {
		*///?} else
		if (!overlay.labyModMenu$getFadeIn() && !LabyModMenuConfig.isMojangLogoAnimationEnabled()
				&& overlay.labyModMenu$getMinecraft().gui.overlay() == null) {
			TitleOpeningController.playOpening();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void labyModMenu$tick(CallbackInfo ci) {
		if (!LabyModMenuConfig.isMojangLogoAnimationEnabled()
				|| ((LoadingOverlayAccessor) this).labyModMenu$getFadeIn()) {
			return;
		}
		ci.cancel();
	}

	@Unique
	private static int backgroundColor(LoadingOverlayAccessor overlay) {
		if (LabyModMenuConfig.isDarkLoadingScreenEnabled()) {
			return CUSTOM_DARK_BACKGROUND;
		}
		return overlay.labyModMenu$getMinecraft().options.darkMojangStudiosBackground().get()
				? DARK_BACKGROUND : LIGHT_BACKGROUND;
	}

	@Unique
	private static void tickFadeIn(LoadingOverlayAccessor overlay, long now) {
		if (overlay.labyModMenu$getFadeIn() && overlay.labyModMenu$getFadeInStart() == -1L) {
			overlay.labyModMenu$setFadeInStart(now);
		}
	}

	@Unique
	private static float fadeOutProgress(LoadingOverlayAccessor overlay, long now) {
		return overlay.labyModMenu$getFadeOutStart() > -1L
				? (now - overlay.labyModMenu$getFadeOutStart()) / 1000.0F : -1.0F;
	}

	@Unique
	private static float fadeInProgress(LoadingOverlayAccessor overlay, long now) {
		return overlay.labyModMenu$getFadeInStart() > -1L
				? (now - overlay.labyModMenu$getFadeInStart()) / 500.0F : -1.0F;
	}

	@Unique
	private static void smoothProgress(LoadingOverlayAccessor overlay) {
		float rawProgress = overlay.labyModMenu$getReload().getActualProgress();
		float target = overlay.labyModMenu$getReload().isDone() ? 1.0F : rawProgress;
		float smoothing = MojangAnimation.isFinished() ? 0.12F : 0.050000012F;
		float progress = overlay.labyModMenu$getCurrentProgress() * (1.0F - smoothing) + target * smoothing;
		overlay.labyModMenu$setCurrentProgress(Mth.clamp(progress, 0.0F, 1.0F));
	}

	@Unique
	private static boolean shouldStartAnimation(LoadingOverlayAccessor overlay) {
		return !MojangAnimation.hasStarted()
				&& overlay.labyModMenu$getReload().isDone();
	}

	@Unique
	private static void holdFadeOutUntilAnimationFinishes(LoadingOverlayAccessor overlay) {
		if (MojangAnimation.hasStarted()
				&& !MojangAnimation.isFinished()
				&& overlay.labyModMenu$getFadeOutStart() != -1L) {
			overlay.labyModMenu$setFadeOutStart(-1L);
		}
	}

	@Unique
	private static void renderProgressBar(GuiGraphicsExtractor graphics, LoadingOverlayAccessor overlay, float fadeOut,
			int backgroundColor) {
		int centerX = graphics.guiWidth() / 2;
		int halfWidth = (int) (Math.min(graphics.guiWidth() * 0.75, graphics.guiHeight()) * 0.5);
		int y = (int) (graphics.guiHeight() * 0.8325);
		drawProgressBar(graphics, centerX - halfWidth, y - 5, centerX + halfWidth, y + 5, 1.0F,
				overlay.labyModMenu$getCurrentProgress(), backgroundColor);
	}

	@Unique
	private static void renderVanillaLogo(GuiGraphicsExtractor graphics) {
		int centerX = graphics.guiWidth() / 2;
		int centerY = graphics.guiHeight() / 2;
		double logoHeight = Math.min(graphics.guiWidth() * 0.75, graphics.guiHeight()) * 0.25;
		int halfHeight = (int) (logoHeight * 0.5);
		double logoWidth = logoHeight * 4.0;
		int halfWidth = (int) (logoWidth * 0.5);

		graphics.blit(RenderPipelines.MOJANG_LOGO, VANILLA_LOGO, centerX - halfWidth, centerY - halfHeight,
				-0.0625F, 0.0F, halfWidth, (int) logoHeight, 120, 60, 120, 120, TEXT_COLOR);
		graphics.blit(RenderPipelines.MOJANG_LOGO, VANILLA_LOGO, centerX, centerY - halfHeight,
				0.0625F, 60.0F, halfWidth, (int) logoHeight, 120, 60, 120, 120, TEXT_COLOR);
	}

	@Unique
	private static void drawProgressBar(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, float alpha,
			float progress, int backgroundColor) {
		int width = x2 - x1;
		int filled = Math.min(Mth.ceil((width - 2) * progress), width - 4);
		int border = applyAlpha(TEXT_COLOR, alpha);
		int background = applyAlpha(backgroundColor, alpha);

		graphics.fill(x1 + 1, y1, x2 - 1, y1 + 1, border);
		graphics.fill(x1 + 1, y2 - 1, x2 - 1, y2, border);
		graphics.fill(x1, y1, x1 + 1, y2, border);
		graphics.fill(x2 - 1, y1, x2, y2, border);
		graphics.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, background);
		graphics.fill(x1 + 2, y1 + 2, x1 + 2 + filled, y2 - 2, border);
	}

	@Unique
	private static int applyAlpha(int color, float alpha) {
		return Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24 | color & 0xFFFFFF;
	}

	@Unique
	private boolean canFinishReload(LoadingOverlayAccessor overlay, boolean fadeInReady) {
		return !labyModMenu$lifecycle.isReloadCompletionDispatched()
				&& overlay.labyModMenu$getReload().isDone()
				&& fadeInReady;
	}

	@Unique
	private void finishReload(LoadingOverlayAccessor overlay, GuiGraphicsExtractor graphics, boolean fadeInReady) {
		labyModMenu$lifecycle.tryCompleteReload(
				overlay.labyModMenu$getReload().isDone(),
				fadeInReady,
				lifecycleContract(overlay, graphics));
	}

	@Unique
	private static LoadingLifecycleContract lifecycleContract(
			LoadingOverlayAccessor overlay, GuiGraphicsExtractor graphics) {
		return new LoadingLifecycleContract() {
			@Override
			public void checkReloadExceptions() {
				overlay.labyModMenu$getReload().checkExceptions();
			}

			@Override
			public void onFinish(Optional<Throwable> failure) {
				overlay.labyModMenu$getOnFinish().accept(failure);
			}

			@Override
			public void initializeCurrentScreen() {
				//? if <26.3 {
				/*if (overlay.labyModMenu$getMinecraft().screen != null) {
					overlay.labyModMenu$getMinecraft().screen.init(
				*///?} else
				if (overlay.labyModMenu$getMinecraft().gui.screen() != null) {
					overlay.labyModMenu$getMinecraft().gui.screen().init(
							graphics.guiWidth(), graphics.guiHeight());
				}
			}

			@Override
			public void removeOverlay() {
				//? if <26.3 {
				/*overlay.labyModMenu$getMinecraft().setOverlay(null);
				*///?} else
				overlay.labyModMenu$getMinecraft().gui.setOverlay(null);
			}
		};
	}

	@Unique
	private static boolean isServerTransition(LoadingOverlayAccessor overlay) {
		//? if <26.3 {
		/*Screen screen = overlay.labyModMenu$getMinecraft().screen;
		*///?} else
		Screen screen = overlay.labyModMenu$getMinecraft().gui.screen();
		return screen instanceof ConnectScreen || TitleOpeningController.isConnectionFlowActive()
				|| overlay.labyModMenu$getMinecraft().level != null;
	}
}
