package labymod_menu.client.mixin;

import labymod_menu.client.internal.TitleOpeningController;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// GenericWaitingScreen and ProgressScreen inherit Screen.extractBackground, so
// ScreenMixin already owns their background cancellation. Mixin injectors do not
// search superclasses; listing those two here makes the whole mixin fail at runtime.
@Mixin({ GenericMessageScreen.class, LevelLoadingScreen.class })
public class TransitionScreenMixin {
	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void labyModMenu$keepDynamicCaveDuringTransition(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float delta, CallbackInfo ci) {
		Screen screen = (Screen) (Object) this;
		if (TitleOpeningController.shouldRenderDynamicScreenBackground()) {
			graphics.blurBeforeThisStratum();
			ci.cancel();
		}
	}
}
