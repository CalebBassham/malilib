package fi.dy.masa.malilib.render.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class TextRenderer implements IResourceManagerReloadListener
{
    public static final String VALID_ASCII_CHARACTERS = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";
    public static final String VANILLA_COLOR_CODES = "0123456789abcdef";
    public static final ResourceLocation ASCII_TEXTURE = new ResourceLocation("textures/font/ascii.png");

    protected static final Glyph EMPTY_GLYPH = new Glyph(ASCII_TEXTURE, 0, 0, 0, 0, 4, 8, 4);
    protected static final ResourceLocation[] UNICODE_PAGE_LOCATIONS = new ResourceLocation[256];

    // This needs to be below the other static fields, because the resource manager reload will access the  other fields!
    public static final TextRenderer INSTANCE = new TextRenderer(Minecraft.getMinecraft().getTextureManager(), ASCII_TEXTURE, false, false);

    protected final Char2ObjectOpenHashMap<Glyph> glyphs = new Char2ObjectOpenHashMap<>();
    protected final Int2ObjectOpenHashMap<List<Glyph>> glyphsBySize = new Int2ObjectOpenHashMap<>();
    protected final WorldVertexBufferUploader vboUploader = new WorldVertexBufferUploader();
    protected final BufferBuilder textBuffer = new BufferBuilder(65536);
    protected final BufferBuilder styleBuffer = new BufferBuilder(8192);
    protected final Random rand = new Random();
    protected final TextureManager textureManager;
    protected final ResourceLocation asciiTexture;
    protected final byte[] glyphWidth = new byte[65536];
    protected final byte[] asciiCharacterWidths = new byte[65536];
    protected final int[] charWidth = new int[256];
    protected final int[] colorCode = new int[32];
    protected boolean anaglyph;
    protected boolean unicode;
    protected boolean buildingStyleBuffer;
    protected boolean buildingTextBuffer;
    protected int fontHeight = 9;
    protected int lineHeight = 10;
    protected int asciiGlyphWidth = 8;
    protected int asciiGlyphHeight = 8;

    public TextRenderer(TextureManager textureManager, ResourceLocation asciiTexture, boolean unicode, boolean anaglyph)
    {
        this.textureManager = textureManager;
        this.asciiTexture = asciiTexture;
        this.unicode = unicode;
        this.anaglyph = anaglyph;

        this.setColorCodes(anaglyph);

        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this);
    }

    protected void setColorCodes(boolean anaglyph)
    {
        TextRendererUtils.setColorCodes(this.colorCode, anaglyph);
    }

    protected void readGlyphSizes()
    {
        TextRendererUtils.readGlyphSizes(this.glyphWidth);
    }

    protected void readFontTexture()
    {
        TextRendererUtils.readCharacterWidthsFromFontTexture(this.asciiTexture, this.charWidth, this::setAsciiGlyphWidth, this::setAsciiGlyphHeight);

        int len = Math.min(VALID_ASCII_CHARACTERS.length(), this.charWidth.length);

        for (int i = 0; i < len; ++i)
        {
            char c = VALID_ASCII_CHARACTERS.charAt(i);
            this.asciiCharacterWidths[c] = (byte) this.charWidth[i];
            this.getGlyphFor(c, false); // generate all the glyphs so that the randomization always has all the valid alternatives available
        }
    }

    protected void setAsciiGlyphWidth(int width)
    {
        this.asciiGlyphWidth = width;
    }

    protected void setAsciiGlyphHeight(int height)
    {
        this.asciiGlyphHeight = height;
    }

    protected ResourceLocation getUnicodePageLocation(int page)
    {
        if (UNICODE_PAGE_LOCATIONS[page] == null)
        {
            UNICODE_PAGE_LOCATIONS[page] = new ResourceLocation(String.format("textures/font/unicode_page_%02x.png", page));
        }

        return UNICODE_PAGE_LOCATIONS[page];
    }

    @Override
    public void onResourceManagerReload(@Nonnull IResourceManager resourceManager)
    {
        // FIXME these should be hooked from the vanilla setter
        Minecraft mc = Minecraft.getMinecraft();
        this.unicode = mc.isUnicode();

        if (mc.gameSettings.anaglyph != this.anaglyph)
        {
            this.anaglyph = mc.gameSettings.anaglyph;
            this.setColorCodes(this.anaglyph);
        }

        this.glyphs.clear();
        this.glyphsBySize.clear();
        Arrays.fill(this.charWidth, 0);
        Arrays.fill(this.glyphWidth, (byte) 0);
        Arrays.fill(this.asciiCharacterWidths, (byte) 0);

        this.readGlyphSizes();
        this.readFontTexture();
    }

    public int getFontHeight()
    {
        return this.fontHeight;
    }

    public int getColorCode(char colorCodeChar)
    {
        int index = VANILLA_COLOR_CODES.indexOf(colorCodeChar);
        return index >= 0 && index < 16 ? this.colorCode[index] : 0;
    }

    public int getCharWidth(char c)
    {
        return this.getGlyphFor(c, false).renderWidth;
    }

    public int getStringWidth(String str)
    {
        int len = str.length();
        int width = 0;

        // TODO §l and §r ??

        for (int i = 0; i < len; ++i)
        {
            width += this.getCharWidth(str.charAt(i));
        }

        return width;
    }

    protected Glyph getGlyphFor(char c, boolean randomize)
    {
        Glyph glyph = this.glyphs.get(c);

        if (glyph == null)
        {
            if (c == ' ')
            {
                glyph = EMPTY_GLYPH;
            }
            else if (c > 0 && this.unicode == false && this.asciiCharacterWidths[c] != 0)
            {
                glyph = this.generateAsciiCharacterGlyph(c);
            }
            else if (this.glyphWidth[c] != 0)
            {
                glyph = this.generateUnicodeCharacterGlyph(c);
            }
            else
            {
                glyph = EMPTY_GLYPH;
            }

            this.glyphs.put(c, glyph);
            this.glyphsBySize.computeIfAbsent(glyph.renderWidth, (w) -> new ArrayList<>()).add(glyph);
        }

        if (randomize)
        {
            List<Glyph> list = this.glyphsBySize.get(glyph.renderWidth);
            glyph = list.get(this.rand.nextInt(list.size()));
        }

        return glyph;
    }

    protected Glyph generateAsciiCharacterGlyph(char c)
    {
        int width = this.asciiCharacterWidths[c];

        // 16 characters per row and column
        float u1 = (float) (c % 16) / 16.0F;
        float v1 = (float) (c / 16) / 16.0F;
        float u2 = u1 + ((float) width / (float) this.asciiGlyphWidth / 16.0F);
        float v2 = v1 + 0.0625F;

        return new Glyph(this.asciiTexture, u1, v1, u2, v2, width, this.asciiGlyphHeight);
    }

    protected Glyph generateUnicodeCharacterGlyph(char c)
    {
        int data = this.glyphWidth[c] & 0xFF;

        if (data == 0)
        {
            return EMPTY_GLYPH;
        }

        // FIXME: unicode glyph and sheet width and height
        float sheetWidth = 256.0F;
        float sheetHeight = 256.0F;

        int startPos = data >>> 4;
        int endPos = data & 0xF;
        int width = endPos - startPos + 1;
        int height = (int) (sheetHeight / 16.0F);

        // 16 glyphs per row and column
        float u1 = (((c & 0x0F) * (sheetWidth / 16.0F)) + startPos) / sheetWidth;
        float v1 = (float) (c & 0xF0) / sheetWidth;
        float u2 = u1 + (float) width / sheetWidth - 0.02F / sheetWidth;
        float v2 = v1 + 0.0625F - 0.02F / sheetWidth;

        return new Glyph(this.getUnicodePageLocation(c >> 8), u1, v1, u2, v2, width / 2, height / 2, width / 2 + 1);
    }

    public void startBuffers()
    {
        if (this.buildingTextBuffer == false)
        {
            this.textBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            this.buildingTextBuffer = true;
        }

        if (this.buildingStyleBuffer == false)
        {
            this.styleBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            this.buildingStyleBuffer = true;
        }
    }

    public void renderBuffers()
    {
        this.renderTextBuffer();

        if (this.buildingStyleBuffer)
        {
            GlStateManager.disableTexture2D();
            this.styleBuffer.finishDrawing();
            this.vboUploader.draw(this.styleBuffer);
            this.buildingStyleBuffer = false;
            GlStateManager.enableTexture2D();
        }
    }

    protected void renderTextBuffer()
    {
        if (this.buildingTextBuffer)
        {
            this.textBuffer.finishDrawing();
            this.vboUploader.draw(this.textBuffer);
            this.buildingTextBuffer = false;
        }
    }

    public void renderText(int x, int y, float z, int defaultColor, boolean shadow, StyledText text)
    {
        this.renderText(x, y, z, defaultColor, shadow, text, this.lineHeight);
    }

    public void renderText(int x, int y, float z, int defaultColor, boolean shadow, StyledText text, int lineHeight)
    {
        this.startBuffers();

        for (StyledTextLine line : text.lines)
        {
            this.renderLineToBuffer(x, y, z, defaultColor, shadow, line);
            y += lineHeight;
        }

        this.renderBuffers();
    }

    public void renderLine(int x, int y, float z, int defaultColor, boolean shadow, StyledTextLine line)
    {
        this.startBuffers();
        this.renderLineToBuffer(x, y, z, defaultColor, shadow, line);
        this.renderBuffers();
    }

    public void renderLineToBuffer(int x, int y, float z, int defaultColor, boolean shadow, StyledTextLine line)
    {
        if (this.textBuffer != null)
        {
            int segmentX = x;
            Color4f defaultColor4f = Color4f.fromColor(defaultColor);
            RenderUtils.color(1f, 1f, 1f, 1f);

            for (StyledTextSegment segment : line.segments)
            {
                segmentX += this.renderTextSegment(segmentX, y, z, defaultColor4f, shadow, segment);
            }
        }
    }

    protected int renderTextSegment(int x, int y, float z, Color4f defaultColor, boolean shadow, StyledTextSegment segment)
    {
        TextStyle style = segment.style;
        Color4f color = style.color != null ? style.color : defaultColor;

        if (style.shadow != null)
        {
            shadow = style.shadow;
        }

        if (shadow)
        {
            Color4f shadowColor = style.shadowColor != null ? style.shadowColor : TextStyle.getDefaultShadowColor(color);
            float offset = this.unicode ? 0.5F : 1.0F;
            this.renderTextSegmentWithColor(x + offset, y + offset, z, shadowColor, segment);
        }

        return this.renderTextSegmentWithColor(x, y, z, color, segment);
    }

    protected int renderTextSegmentWithColor(float x, float y, float z, Color4f color, StyledTextSegment segment)
    {
        TextStyle style = segment.style;
        int renderWidth = this.renderString(x, y, z, segment.text, color, style, this.textBuffer);

        if (style.underline)
        {
            RenderUtils.renderRectangleBatched(x - 1F, y + this.fontHeight - 1F, z, segment.renderWidth, 1, color, this.styleBuffer);
        }

        if (style.strikeThrough)
        {
            RenderUtils.renderRectangleBatched(x - 1F, y + this.fontHeight / 2.0F - 1F, z, segment.renderWidth + 1, 1, color, this.styleBuffer);
        }

        return renderWidth;
    }

    protected int renderString(float x, float y, float z, String str, Color4f color, TextStyle style, BufferBuilder buffer)
    {
        int len = str.length();
        int renderWidth = 0;

        for (int i = 0; i < len; ++i)
        {
            char c = str.charAt(i);
            Glyph glyph = this.getGlyphFor(c, style.random);
            renderWidth += this.renderGlyph(x + renderWidth, y, z, glyph, color, style, buffer);
        }

        return renderWidth;
    }

    protected int renderGlyph(float x, float y, float z, Glyph glyph, Color4f color, TextStyle style, BufferBuilder buffer)
    {
        if (glyph == EMPTY_GLYPH)
        {
            return glyph.renderWidth;
        }

        float slant = style.italic ? 1.0F : 0.0F;
        float w = (float) glyph.width;
        int renderWidth = glyph.renderWidth;
        float h = glyph.height;
        float u1 = glyph.u1;
        float u2 = glyph.u2;
        float v1 = glyph.v1;
        float v2 = glyph.v2;

        this.textureManager.bindTexture(glyph.texture); // FIXME this should be per text segment

        buffer.pos(x     + slant, y    , z).tex(u1, v1).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(x     - slant, y + h, z).tex(u1, v2).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(x + w - slant, y + h, z).tex(u2, v2).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(x + w + slant, y    , z).tex(u2, v1).color(color.r, color.g, color.b, color.a).endVertex();

        if (style.bold)
        {
            x += this.unicode ? 0.5F : 1.0F;
            renderWidth += 1;

            buffer.pos(x     + slant, y    , z).tex(u1, v1).color(color.r, color.g, color.b, color.a).endVertex();
            buffer.pos(x     - slant, y + h, z).tex(u1, v2).color(color.r, color.g, color.b, color.a).endVertex();
            buffer.pos(x + w - slant, y + h, z).tex(u2, v2).color(color.r, color.g, color.b, color.a).endVertex();
            buffer.pos(x + w + slant, y    , z).tex(u2, v1).color(color.r, color.g, color.b, color.a).endVertex();
        }

        return renderWidth;
    }
}
