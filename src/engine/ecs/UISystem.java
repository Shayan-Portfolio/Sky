package engine.ecs;

import engine.Surface;

import engine.SystemState;
import engine.asset.AssetRegistry;
import engine.gameui.*;
import engine.graphics.*;
import engine.graphics.pipelines.ScreenSpaceFeatures;
import engine.graphics.text.*;
import org.joml.*;

import java.lang.Math;
import java.nio.ByteBuffer;

public class UISystem extends ActorSystem {

    private Renderer renderer;
    private RenderPipeline renderPipeline;
    private Scene scene;
    private int quadCount = 0;
    private ByteBuffer vertexBufferData, indexBufferData;
    private Matrix4f transform = new Matrix4f();
    private Vector4f origin = new Vector4f();
    private Surface surface;
    private GfxPlatform gfxPlatform;
    private Theme theme;
    private TextureBindings textureBindings;
    private Matrix4f lineRotation = new Matrix4f();



    public UISystem(Renderer renderer, RenderPipeline renderPipeline, Surface surface, Scene scene, Theme theme) {
        this.renderer = renderer;
        this.renderPipeline = renderPipeline;
        this.surface = surface;
        this.scene = scene;
        this.theme = theme;


        textureBindings = new TextureBindings();





        //Gfx Platform
        {
            gfxPlatform = new GfxPlatform() {
                @Override
                public int getMouseX() {
                    if(Session.getSurface().isMouseCaptured()) return 0;
                    return (int) surface.getMousePos().x;
                }

                @Override
                public int getMouseY() {
                    if(Session.getSurface().isMouseCaptured()) return 0;
                    return (int) surface.getMousePos().y;
                }

                @Override
                public boolean isMousePressed(int mouseButton) {
                    return surface.getMousePressed(mouseButton);
                }

                @Override
                public void drawRect(float x, float y, float w, float h, Color color) {
                    UISystem.this.drawQuad(
                            x,
                            y,
                            w,
                            h,
                            -1, -1,
                            -1, -1,
                            -1, -1,
                            -1, -1,
                            -1,
                            -1,
                            -1,
                            color
                    );
                }

                @Override
                public void drawArc(float x, float y, float w, float h, float range, float thickness, Color color) {
                    UISystem.this.drawQuad(
                            x,
                            y,
                            w,
                            h,
                            -1, 1,
                            -1, -1,
                            1, 1,
                            1, -1,
                            -2,
                            thickness,
                            range,
                            color
                    );
                }

                @Override
                public void drawRectLines(float x, float y, float w, float h, int thickness, Color color) {
                    drawRect(x - ((float) thickness / 2), y, thickness, h, color);
                    //Top
                    drawRect(x, y - ((float) thickness / 2), w, thickness, color);
                    //Bottom
                    drawRect(x, y - ((float) thickness / 2) + h, w, thickness, color);
                    //Right
                    drawRect(x - ((float) thickness / 2) + w, y, thickness, h, color);
                }

                @Override
                public void drawLine(float x1, float y1, float x2, float y2, Color color) {
                    float dx = x2 - x1;
                    float dy = y2 - y1;

                    float angle = (float) Math.atan2(dy, dx);

                    setOrigin(x1, y1);
                    setTransform(transform.rotateZ(angle));

                    float hypotenuse = (float) Math.sqrt((dx * dx) + (dy * dy));

                    drawRect(x1, y1 - 2, hypotenuse, 4, color);

                    setTransform(transform.rotateZ(-angle));

                    setOrigin(0, 0);
                }


                public void drawTexture(float x,
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
                                        Sampler sampler, boolean msdf) {

                    int index = textureBindings.getTextureBinding(texture);

                    ScreenSpaceFeatures screenSpaceFeatures = renderPipeline.getFeatures(ScreenSpaceFeatures.class);
                    screenSpaceFeatures.getShaderProgram().setTextures(renderer.getFrameIndex(), new DescriptorUpdate<>("input_textures", texture).arrayIndex(index));
                    screenSpaceFeatures.getShaderProgram().setSamplers(renderer.getFrameIndex(), new DescriptorUpdate<>("input_samplers", sampler).arrayIndex(index));


                    drawQuad(
                            x,
                            y,
                            w,
                            h,
                            uvtlx,
                            uvtly,
                            uvblx,
                            uvbly,
                            uvtrx,
                            uvtry,
                            uvbrx,
                            uvbry,
                            msdf ? -3 : 0,
                            index,
                            op1,
                            color
                    );
                }

                @Override
                public void setOrigin(float x, float y) {
                    UISystem.this.setOrigin(x, y);
                }

                @Override
                public Vector4f getOrigin() {
                    return origin;
                }

                @Override
                public void setTransform(Matrix4f transform) {
                    UISystem.this.transform.set(transform);
                }

                @Override
                public void drawTexture(float x, float y, float w, float h, Color color, Texture texture, Sampler sampler) {
                    drawTexture(x, y, w, h, 0, 0,
                            0, 1,
                            1, 0,
                            1, 1,
                            -1,
                            color,
                            texture,
                            sampler,
                            false
                    );
                }

                private void drawGlyph(float x, float y, float w, float h, MsdfFont msdfFont, MsdfJsonLoader.Character character, Color color) {

                    MsdfJsonLoader.MsdfData msdfData = msdfFont.getMSDFData();
                    int msdfScreenPxRange = (int) ((32 / msdfData.width) * msdfData.size);

                    drawTexture(
                            x,
                            y,
                            w,
                            h,
                            character.atlasBounds.left / msdfData.width,
                            1 - character.atlasBounds.top / msdfData.height,
                            character.atlasBounds.left / msdfData.width,
                            1 - character.atlasBounds.bottom / msdfData.height,
                            character.atlasBounds.right / msdfData.width,
                            1 - character.atlasBounds.top / msdfData.height,
                            character.atlasBounds.right / msdfData.width,
                            1 - character.atlasBounds.bottom / msdfData.height,
                            msdfScreenPxRange,
                            color,
                            msdfFont.getTexture(),
                            msdfFont.getSampler(),
                            true
                    );
                }

                @Override
                public void drawString(float x, float y, String text, MsdfFont msdfFont, TextEffect textEffect, Color color) {
                    if(textEffect != null)
                        textEffect.update();

                    float xl = 0;
                    float yl = y + (msdfFont.getMSDFData().lineHeight + msdfFont.getMSDFData().descender) * msdfFont.getMSDFData().size;
                    float spaceXAdvance = msdfFont.getMSDFData().characters[' '].advance;

                    for (int i = 0; i < text.length(); i++) {
                        char c = text.charAt(i);

                        if(c == '\n') {
                            yl += msdfFont.getMSDFData().lineHeight * msdfFont.getMSDFData().size;
                            xl = 0;
                            continue;
                        }
                        if(c == '\t') {
                            xl = msdfFont.getTabWidth() * spaceXAdvance;
                            continue;
                        }

                        MsdfJsonLoader.Character character = msdfFont.getMSDFData().characters[c];
                        if(character == null) character = msdfFont.getMSDFData().characters['?'];

                        MsdfJsonLoader.Rect planeBounds = character.planeBounds;
                        if(planeBounds != null) {

                            float sw = (planeBounds.right - planeBounds.left) * msdfFont.getMSDFData().size;
                            float sh = (planeBounds.top - planeBounds.bottom) * msdfFont.getMSDFData().size;
                            float yo = planeBounds.bottom * msdfFont.getMSDFData().size;

                            float effectOffsetX = 0, effectOffsetY = 0;

                            if(textEffect != null) {
                                Vector2f effectOffset = textEffect.offset(i, text.length());
                                effectOffsetX = effectOffset.x;
                                effectOffsetY = effectOffset.y;
                            }

                            drawGlyph(
                                    x + xl + effectOffsetX,
                                    yl - sh - yo + effectOffsetY,
                                    sw,
                                    sh,
                                    msdfFont,
                                    character,
                                    color
                            );
                        }

                        xl += character.advance * msdfFont.getMSDFData().size;
                    }


                }

                @Override
                public Theme getTheme() {
                    return theme;
                }
            };
        }

    }

    @Override
    public void run(Actor root) {
        ScreenSpaceFeatures screenSpaceFeatures = renderPipeline.getFeatures(ScreenSpaceFeatures.class);
        vertexBufferData = screenSpaceFeatures.getVertexBuffers()[renderer.getFrameIndex()].get();
        vertexBufferData.clear();
        indexBufferData = screenSpaceFeatures.getIndexBuffers()[renderer.getFrameIndex()].get();
        indexBufferData.clear();

        transform = new Matrix4f();
        setOrigin(0, 0);

        drawQuad(
                0,
                0,
                renderer.getWidth(),
                renderer.getHeight(),
                0, 0,
                0, 1,
                1, 0,
                1, 1,
                0,
                0,
                -1,
                new Color(1, 1, 1, 1)
        );




        root.previsitAllActors(actor -> {
            if (actor.has(UIComponent.class)) {
                UIComponent uiComponent = actor.getComponent(UIComponent.class);
                if (!uiComponent.active) {

                    uiComponent.loop = new Loop();
                    uiComponent.loop.setWidget(uiComponent.widget);
                    uiComponent.loop.setGfxPlatform(gfxPlatform);

                    uiComponent.active = true;
                }

                if (uiComponent.rect2D.isPresent()) {
                    Rect2D rect2D = uiComponent.rect2D.get();
                    uiComponent.loop.update(
                            (int) rect2D.x,
                            (int) rect2D.y,
                            (int) rect2D.w,
                            (int) rect2D.h
                    );
                } else {
                    uiComponent.loop.update(
                            0, 0
                    );
                }
            }


        });


        screenSpaceFeatures.setIndexCount(6 * quadCount);
        quadCount = 0;
    }


    private void setOrigin(float x, float y) {
        origin.set(x, y, 0, 1);
    }


    private void drawQuad(float x,
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

                          int shapeMode,
                          float op0,
                          float op1,
                          Color color) {

        Vector4f
                topLeft = new Vector4f(x, y, 0, 1).sub(origin).mul(transform).add(origin),
                bottomLeft = new Vector4f(x, y + h, 0, 1).sub(origin).mul(transform).add(origin),
                bottomRight = new Vector4f(x + w, y + h, 0, 1).sub(origin).mul(transform).add(origin),
                topRight = new Vector4f(x + w, y, 0, 1).sub(origin).mul(transform).add(origin);

        vertexBufferData.putFloat(topLeft.x);
        vertexBufferData.putFloat(topLeft.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvtlx);
        vertexBufferData.putFloat(uvtly);
        vertexBufferData.putFloat(shapeMode);
        vertexBufferData.putFloat(op0);
        vertexBufferData.putFloat(op1);

        vertexBufferData.putFloat(bottomLeft.x);
        vertexBufferData.putFloat(bottomLeft.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvblx);
        vertexBufferData.putFloat(uvbly);
        vertexBufferData.putFloat(shapeMode);
        vertexBufferData.putFloat(op0);
        vertexBufferData.putFloat(op1);

        vertexBufferData.putFloat(bottomRight.x);
        vertexBufferData.putFloat(bottomRight.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvbrx);
        vertexBufferData.putFloat(uvbry);
        vertexBufferData.putFloat(shapeMode);
        vertexBufferData.putFloat(op0);
        vertexBufferData.putFloat(op1);

        vertexBufferData.putFloat(topRight.x);
        vertexBufferData.putFloat(topRight.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvtrx);
        vertexBufferData.putFloat(uvtry);
        vertexBufferData.putFloat(shapeMode);
        vertexBufferData.putFloat(op0);
        vertexBufferData.putFloat(op1);

        indexBufferData.putInt(0 + (4 * quadCount));
        indexBufferData.putInt(1 + (4 * quadCount));
        indexBufferData.putInt(2 + (4 * quadCount));
        indexBufferData.putInt(2 + (4 * quadCount));
        indexBufferData.putInt(3 + (4 * quadCount));
        indexBufferData.putInt(0 + (4 * quadCount));
        quadCount++;
    }

    @Override
    public void dispose() {

    }
}
