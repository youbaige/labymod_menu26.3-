package labymod_menu.client.mixin;

import labymod_menu.client.internal.TitleOpeningController;
//? if <26.3 {
/*import net.minecraft.client.DeltaTracker;
*///?}
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	//? if <26.3 {
	/*@Inject(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
			shift = At.Shift.BEFORE))
	private void labyModMenu$renderTitleSchematicBackground(DeltaTracker deltaTracker, boolean renderLevel,
			CallbackInfo ci) {
	*///?} else
	@Inject(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V",
			shift = At.Shift.BEFORE))
	private void labyModMenu$renderTitleSchematicBackground(CallbackInfo ci) {
		TitleOpeningController.renderWorldBackground();
	}
}
