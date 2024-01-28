package malilib.render.buffer;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;

import malilib.util.game.wrap.RenderWrap;

public class VertexFormat
{
    protected final boolean hasTexture;
    protected final int size;
    protected final int colorOffset;
    protected final int normalOffset;
    protected final int positionOffset;
    protected final int uvOffset;
    protected final int padding;

    public VertexFormat(int positionOffset, int uvOffset, int colorOffset, int normalOffset, int padding)
    {
        this.positionOffset = positionOffset;
        this.uvOffset = uvOffset;
        this.colorOffset = colorOffset;
        this.normalOffset = normalOffset;
        this.padding = padding;

        int size = 0;
        size = positionOffset >= 0 ? size + 3 * 4 : size;
        size = uvOffset     >= 0 ? size + 2 * 4 : size;
        size = colorOffset  >= 0 ? size + 4 * 1 : size;
        size = normalOffset >= 0 ? size + 3 * 1 : size;

        this.size = size + padding;
        this.hasTexture = uvOffset >= 0;
    }

    public int getSize()
    {
        return this.size;
    }

    public boolean hasTexture()
    {
        return this.hasTexture;
    }

    public int getColorOffset()
    {
        return this.colorOffset;
    }

    public int getNormalOffset()
    {
        return this.normalOffset;
    }

    public int getPositionOffset()
    {
        return this.positionOffset;
    }

    public int getUvOffset()
    {
        return this.uvOffset;
    }

    public void setupDraw(ByteBuffer byteBuffer)
    {
        int stride = this.getSize();

        if (this.positionOffset >= 0)
        {
            byteBuffer.position(this.positionOffset);
            RenderWrap.vertexPointer(3, GL11.GL_FLOAT, stride, byteBuffer);
            RenderWrap.enableClientState(GL11.GL_VERTEX_ARRAY);
        }

        if (this.uvOffset >= 0)
        {
            byteBuffer.position(this.uvOffset);
            int texIndex = 0;
            RenderWrap.setClientActiveTexture(RenderWrap.DEFAULT_TEX_UNIT + texIndex);
            RenderWrap.texCoordPointer(2, GL11.GL_FLOAT, stride, byteBuffer);
            RenderWrap.enableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            RenderWrap.setClientActiveTexture(RenderWrap.DEFAULT_TEX_UNIT);
        }

        if (this.colorOffset >= 0)
        {
            byteBuffer.position(this.colorOffset);
            RenderWrap.colorPointer(4, GL11.GL_UNSIGNED_BYTE, stride, byteBuffer);
            RenderWrap.enableClientState(GL11.GL_COLOR_ARRAY);
        }

        if (this.normalOffset >= 0)
        {
            byteBuffer.position(this.normalOffset);
            RenderWrap.normalPointer(GL11.GL_BYTE, stride, byteBuffer);
            RenderWrap.enableClientState(GL11.GL_NORMAL_ARRAY);
        }
    }

    public void disableAfterDraw()
    {
        if (this.positionOffset >= 0)
        {
            RenderWrap.disableClientState(GL11.GL_VERTEX_ARRAY);
        }

        if (this.uvOffset >= 0)
        {
            int texIndex = 0;
            RenderWrap.setClientActiveTexture(RenderWrap.DEFAULT_TEX_UNIT + texIndex);
            RenderWrap.disableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            RenderWrap.setClientActiveTexture(RenderWrap.DEFAULT_TEX_UNIT);
        }

        if (this.colorOffset >= 0)
        {
            RenderWrap.disableClientState(GL11.GL_COLOR_ARRAY);
            //RenderWrap.resetColor();
        }

        if (this.normalOffset >= 0)
        {
            RenderWrap.disableClientState(GL11.GL_NORMAL_ARRAY);
        }
    }
}
