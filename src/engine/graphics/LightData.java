package engine.graphics;

import org.joml.Matrix4f;

public class LightData {
    public Matrix4f view, proj;
    public Matrix4f invView, invProj;
    public boolean invertY;
    public RenderTarget renderTarget;
    public float attenuationConstant = 1f;
    public float attenuationLinear = 0.007f;
    public float attenuationQuadratic = 0.0002f;
    public Color color = Color.WHITE;
    public float shadowNormalOffsetBias = 0.0001f;
    public float shadowTestOffsetBias = 0.00001f;

    public LightData(Matrix4f view, Matrix4f proj, boolean invertY, Color color) {
        this.view = view;
        this.proj = proj;
        this.invView = view.invert(new Matrix4f());
        this.invProj = proj.invert(new Matrix4f());
        this.invertY = invertY;
        this.color = color;
    }
}