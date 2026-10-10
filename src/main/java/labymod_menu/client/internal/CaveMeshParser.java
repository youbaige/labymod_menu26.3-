package labymod_menu.client.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * 解析 AnimatedMojang 的 AMCV0005 洞穴网格。
 *
 * 二进制布局（全部小端）：
 *   头部 24 字节 = "AMCV0005"(8) + u32 顶点数(4) + 3x u32 边界(12)
 *   顶点 36 字节 = pos3f(0-11) + rgb3f 浮点[0,1](12-23) + uv2f 已按 u/256、v/768 归一化(24-31) + flags u32(32-35)
 *
 * 输出为 26.3 的 POSITION_TEX_COLOR 顶点（24 字节）：
 *   pos3f(0-11) + uv2f(12-19) + RGBA8(20-23)
 *
 * flags &amp; 0x10000 表示岩浆顶点（UV 取自 atlas 岩浆条带，需要按帧偏移 v）。
 */
public final class CaveMeshParser {
    public static final int FLAG_LAVA = 0x10000;
    private static final String MAGIC = "AMCV0005";
    private static final int HEADER_SIZE = 24;
    private static final int VERTEX_SIZE_IN = 36;
    private static final int VERTEX_SIZE_OUT = 24;
    /** 岩浆动画帧数（atlas 岩浆条带共 32 帧，每帧高 16px / 768）。 */
    public static final int LAVA_FRAME_COUNT = 32;
    /** 岩浆条带在 atlas 中的起始 v（像素，纹理高 768）。 */
    public static final float LAVA_V_BASE = 256.0f / 768.0f;
    private static final float LAVA_FRAME_STEP = 16.0f / 768.0f;

    private CaveMeshParser() {
    }

    public record ParsedMesh(ByteBuffer staticVertices, int staticVertexCount,
                             ByteBuffer lavaVertices, int lavaVertexCount) {
    }

    public static ParsedMesh parse(InputStream in) throws IOException {
        byte[] all = in.readAllBytes();
        ByteBuffer src = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
        if (all.length < HEADER_SIZE) {
            throw new IOException("Cave mesh too small: " + all.length);
        }
        byte[] magic = new byte[8];
        src.get(magic);
        if (!MAGIC.equals(new String(magic, StandardCharsets.US_ASCII))) {
            throw new IOException("Bad cave mesh magic: " + new String(magic, StandardCharsets.US_ASCII));
        }
        int count = src.getInt();
        src.getInt(); // 边界 x
        src.getInt(); // 边界 y
        src.getInt(); // 边界 z
        long expected = HEADER_SIZE + (long) count * VERTEX_SIZE_IN;
        if (all.length < expected) {
            throw new IOException("Cave mesh truncated: expected " + expected + " bytes, got " + all.length);
        }

        int staticCount = 0;
        int lavaCount = 0;
        for (int i = 0; i < count; i++) {
            src.position(HEADER_SIZE + i * VERTEX_SIZE_IN + 32);
            if ((src.getInt() & FLAG_LAVA) != 0) {
                lavaCount++;
            } else {
                staticCount++;
            }
        }

        // 必须 direct：GpuBuffer 创建/上传走 native 指针，heap buffer 会产生非法地址（驱动 segfault）
        ByteBuffer out = ByteBuffer.allocateDirect(count * VERTEX_SIZE_OUT).order(ByteOrder.nativeOrder());
        ByteBuffer lavaOut = ByteBuffer.allocateDirect(Math.max(1, lavaCount) * VERTEX_SIZE_OUT).order(ByteOrder.nativeOrder());

        for (int i = 0; i < count; i++) {
            int base = HEADER_SIZE + i * VERTEX_SIZE_IN;
            src.position(base);
            float x = src.getFloat();
            float y = src.getFloat();
            float z = src.getFloat();
            float r = src.getFloat();
            float g = src.getFloat();
            float b = src.getFloat();
            float u = src.getFloat();
            float v = src.getFloat();
            int flags = src.getInt();

            ByteBuffer dst = (flags & FLAG_LAVA) != 0 ? lavaOut : out;
            dst.putFloat(x).putFloat(y).putFloat(z);
            dst.putFloat(u).putFloat(v);
            dst.put((byte) clampByte(r));
            dst.put((byte) clampByte(g));
            dst.put((byte) clampByte(b));
            dst.put((byte) 0xFF);
        }

        out.flip();
        lavaOut.flip();
        return new ParsedMesh(out, staticCount, lavaOut, lavaCount);
    }

    /**
     * 生成岩浆顶点在指定帧的数据：v_new = v_base + frame * 16/768（直接改写每个顶点 offset 16 处的 v）。
     */
    public static ByteBuffer lavaFrame(ByteBuffer lavaBase, int frame) {
        float offset = LAVA_FRAME_STEP * frame;
        ByteBuffer copy = ByteBuffer.allocateDirect(lavaBase.remaining()).order(ByteOrder.nativeOrder());
        copy.put(lavaBase.duplicate().order(ByteOrder.nativeOrder()));
        copy.flip();
        while (copy.position() + VERTEX_SIZE_OUT <= copy.limit()) {
            int p = copy.position();
            copy.putFloat(p + 16, copy.getFloat(p + 16) + offset);
            copy.position(p + VERTEX_SIZE_OUT);
        }
        copy.position(0);
        return copy;
    }

    private static int clampByte(float c) {
        int i = Math.round(c * 255.0f);
        return Math.max(0, Math.min(255, i));
    }
}
