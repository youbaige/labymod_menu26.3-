package labymod_menu.client.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 RenderPipelines 中 private 的 GUI_TEXTURED_SNIPPET，
 * 供洞穴网格管线派生（保留 core position_tex_color shader、半透明混合与顶点格式）。
 */
@Mixin(value = RenderPipelines.class, remap = false)
public interface RenderPipelinesAccessor {

    @Accessor(value = "GUI_TEXTURED_SNIPPET", remap = false)
    static RenderPipeline.Snippet labyModMenu$guiTexturedSnippet() {
        throw new AssertionError();
    }
}
