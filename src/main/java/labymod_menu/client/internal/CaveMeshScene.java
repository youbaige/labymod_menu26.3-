package labymod_menu.client.internal;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import labymod_menu.client.LabyModMenuClient;
import labymod_menu.client.mixin.RenderPipelinesAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.util.Util;
import org.joml.Matrix4f;

/**
 * 旧版 cavebg 的 AMCV0005 网格洞穴渲染适配层。
 * 直接在主渲染目标上绘制洞穴网格（不 clear、无离屏 blit，含岩浆 UV 帧动画），
 * 对外暴露与 SchematicScene 相同形式的 renderDirectAt 入口供 TitleOpeningController 无缝切换。
 *
 * 相机语义：调用方按 CameraProfiles 的数组顺序传入 (x, y, z, yaw, pitch, roll)，
 * 直接以 rotateX(pitch)、rotateY(yaw) 构造视图矩阵（与融合版既有相机预设一致）。
 * 注意：若把 pitch/yaw 互换，标题屏（TITLE）会有约 24% 的画面完全不被网格覆盖（纯黑）。
 *
 * 深度：GUI_TEXTURED_SNIPPET 本身不含深度状态（深度测试/写入全关，GUI 靠绘制顺序），
 * 直接拿它画 3D 网格会导致三角形互相覆盖（表现为只剩部分面，如只有顶面）。
 * 因此这里显式补上 DepthStencilState.DEFAULT（GREATER_THAN_OR_EQUAL + 写深度，MC 26.3 为反向 Z），
 * 并在渲染通道中把深度清到 0.0（反向 Z 的远平面）。
 */
public final class CaveMeshScene {

    /** 派生自 GUI_TEXTURED_SNIPPET（经 accessor 暴露）：自带 position_tex_color shader、TRANSLUCENT blend、POSITION_TEX_COLOR 格式；改 TRIANGLES + 双面。 */
    private static final RenderPipeline CAVE_PIPELINE = RenderPipeline.builder(
        RenderPipelinesAccessor.labyModMenu$guiTexturedSnippet())
        .withLocation("labymod_menu/pipeline/cave_mesh")
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withCull(false)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .build();

    private static final float PROJECTION_FOV = 50.0F;
    private static final float PROJECTION_Z_NEAR = 0.05F;
    private static final float PROJECTION_Z_FAR = 120.0F;
    private static final long LAVA_FRAME_MS = 300L;
    private static final float DEG_TO_RAD = (float) Math.PI / 180.0F;

    private static CaveMeshScene instance;

    private boolean initialized;
    private boolean failed;
    private GpuBuffer staticBuffer;
    private GpuBuffer lavaBuffer;
    private ByteBuffer lavaBase;
    private int staticCount;
    private int lavaCount;
    private GpuTexture atlasTexture;
    private GpuTextureView atlasView;
    private GpuSampler atlasSampler;
    private final Projection projection = new Projection();
    private final ProjectionMatrixBuffer projectionUbo = new ProjectionMatrixBuffer("labymod_menu_cave_projection");
    private int currentLavaFrame = -1;

    private CaveMeshScene() {
    }

    static CaveMeshScene get() {
        if (instance == null) {
            instance = new CaveMeshScene();
        }
        return instance;
    }

    /** 懒初始化：首次渲染时从 classpath 加载 mesh + atlas。失败只报一次，之后静默跳过。 */
    private void ensureInitialized() {
        if (initialized || failed) {
            return;
        }
        initialized = true;
        try {
            CaveMeshParser.ParsedMesh mesh;
            try (InputStream in = CaveMeshScene.class.getResourceAsStream("/assets/labymod_menu/cave_mesh.bin")) {
                if (in == null) {
                    throw new IllegalStateException("Missing resource: /assets/labymod_menu/cave_mesh.bin");
                }
                mesh = CaveMeshParser.parse(in);
            }
            staticCount = mesh.staticVertexCount();
            lavaCount = mesh.lavaVertexCount();
            LabyModMenuClient.LOGGER.info("Cave mesh parsed: {} static + {} lava vertices", staticCount, lavaCount);
            staticBuffer = createVertexBuffer("labymod_menu_cave_static", mesh.staticVertices(), false);
            lavaBase = mesh.lavaVertices();
            lavaBase.rewind();
            lavaBuffer = createVertexBuffer("labymod_menu_cave_lava", lavaBase, true);

            try (InputStream in = CaveMeshScene.class.getResourceAsStream("/assets/labymod_menu/textures/cave/atlas.png")) {
                if (in == null) {
                    throw new IllegalStateException("Missing resource: /assets/labymod_menu/textures/cave/atlas.png");
                }
                NativeImage image = NativeImage.read(in);
                GpuDevice device = RenderSystem.getDevice();
                atlasTexture = device.createTexture(() -> "labymod_menu_cave_atlas", 5, GpuFormat.RGBA8_UNORM,
                    image.getWidth(), image.getHeight(), 1, 1);
                atlasSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                atlasView = device.createTextureView(atlasTexture);
                // 对齐参考工程惯例：帧中路径的 encoder 命令必须显式 submit，否则可能永远不上传
                var uploadEncoder = device.createCommandEncoder();
                uploadEncoder.writeToTexture(atlasTexture, image);
                uploadEncoder.submit();
                image.close();
            }
            LabyModMenuClient.LOGGER.info("Cave atlas uploaded");
        } catch (Exception e) {
            failed = true;
            LabyModMenuClient.LOGGER.error("Failed to initialize cave mesh background", e);
        }
    }

    private static GpuBuffer createVertexBuffer(String label, ByteBuffer data, boolean updatable) {
        // USAGE_VERTEX(32) | USAGE_COPY_DST(8)（岩浆帧需写回）
        int usage = 32 | (updatable ? 8 : 0);
        data.rewind();
        return RenderSystem.getDevice().createBuffer(() -> label, usage, data);
    }

    /** 岩浆 UV 帧动画：v_base + frame*16/768，300ms 一帧。仅在帧号变化时提交写入。 */
    private void updateLavaFrame() {
        if (lavaCount == 0) {
            return;
        }
        int frame = (int) ((Util.getMillis() / LAVA_FRAME_MS) % CaveMeshParser.LAVA_FRAME_COUNT);
        if (frame == currentLavaFrame) {
            return;
        }
        currentLavaFrame = frame;
        ByteBuffer frameData = CaveMeshParser.lavaFrame(lavaBase, frame);
        var uploadEncoder = RenderSystem.getDevice().createCommandEncoder();
        uploadEncoder.writeToBuffer(lavaBuffer.slice(), frameData);
        uploadEncoder.submit();
    }

    /**
     * 与 SchematicScene.renderDirectAt 对齐的入口（time/openingTime 保留占位，旧网格无风摆）。
     * yaw/pitch 按融合版 CameraProfiles 顺序传入（见类注释）。
     */
    public void renderDirectAt(float x, float y, float z, float yaw, float pitch, float roll,
            float time, float openingTime) {
        renderDirectAt(x, y, z, yaw, pitch, roll, time, openingTime, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    /**
     * 带相机浮动修饰器的入口（对应 LabyMod 原版 CinematicCamera.positionModifier）。
     * 修饰器是作用在主相机之外的“外层”变换，合成顺序与 CinematicCamera.setup 一致：
     * Rz(modRoll) · Rx(modPitch) · Ry(modYaw) · T(0, modY, 0) · 主相机视图矩阵。
     */
    public void renderDirectAt(float x, float y, float z, float yaw, float pitch, float roll,
            float time, float openingTime, float modYaw, float modPitch, float modRoll, float modY) {
        ensureInitialized();
        if (failed || staticCount == 0 || atlasView == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        WindowRenderState window = minecraft.gameRenderer.gameRenderState().windowRenderState;

        // 岩浆帧更新放在 render pass 之外提交
        updateLavaFrame();

        this.projection.setupPerspective(PROJECTION_Z_NEAR, PROJECTION_Z_FAR, PROJECTION_FOV, window.width, window.height);
        RenderSystem.backupProjectionMatrix();
        try {
            RenderSystem.setProjectionMatrix(this.projectionUbo.getBuffer(this.projection), ProjectionType.PERSPECTIVE);

            // 视图矩阵：modRoll(Z) -> modPitch(X) -> modYaw(Y) -> modY 平移 -> 主相机；主相机 roll(Z) -> pitch(X) -> yaw(Y) -> 翻转 Y -> 平移至相机
            Matrix4f view = new Matrix4f()
                .rotationZ(modRoll * DEG_TO_RAD)
                .rotateX(modPitch * DEG_TO_RAD)
                .rotateY(modYaw * DEG_TO_RAD)
                .translate(0.0F, modY, 0.0F)
                .rotationZ(roll * DEG_TO_RAD)
                .rotateX(pitch * DEG_TO_RAD)
                .rotateY(yaw * DEG_TO_RAD)
                .rotateY((float) Math.PI)
                .translate(-x, -y, -z);
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(view);

            RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            try (RenderPass pass = encoder.createRenderPass(
                () -> "CaveMeshBackground", target.getColorTextureView(), Optional.empty(),
                target.getDepthTextureView(), OptionalDouble.of(0.0))) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(CAVE_PIPELINE));
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setUniform("Sampler0", atlasView, atlasSampler);
                pass.setVertexBuffer(0, staticBuffer.slice());
                pass.draw(staticCount, 1, 0, 0);
                if (lavaCount > 0) {
                    pass.setVertexBuffer(0, lavaBuffer.slice());
                    pass.draw(lavaCount, 1, 0, 0);
                }
            }
            encoder.submit();
        } finally {
            RenderSystem.restoreProjectionMatrix();
        }
    }
}
