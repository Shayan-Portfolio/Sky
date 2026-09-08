package engine.gameui;

import engine.graphics.Color;
import engine.graphics.Sampler;
import engine.graphics.Texture;
import engine.graphics.text.MsdfFont;
import engine.graphics.text.TextEffect;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public abstract class UIPainter {
    public abstract void drawRoundRect(float x, float y, float w, float h, float radius, Color c0, Color c1, Color c2, Color c3);

    public abstract void drawRect(float x, float y, float w, float h, Color color);

    public abstract void drawArc(float x, float y, float w, float h, float start, float end, Color color);
    public abstract void drawRectLines(float x, float y, float w, float h, int thickness, Color color);
    public abstract void drawLine(float x1, float y1, float x2, float y2, Color color);
    public abstract void drawTexture(float x,
                             float y,
                             float w,
                             float h,

                             float uvtlx,
                             float uvtly,

                             float uvblx,
                             float uvbly,

                             float uvtrx,
                             float uvtry,

                             float uvbrx,
                             float uvbry,
                             float op1,
                             Color color,
                             Texture texture,
                             Sampler sampler, boolean msdf);
    public abstract void setOrigin(float x, float y);
    public abstract Vector4f getOrigin();
    public abstract void setTransform(Matrix4f transform);
    public abstract void drawTexture(float x, float y, float  w, float h, Color color, Texture texture, Sampler sampler);
    public abstract void drawString(float x, float y, String text, MsdfFont font, TextEffect textEffect, Color color);
    public abstract Theme getTheme();
}
