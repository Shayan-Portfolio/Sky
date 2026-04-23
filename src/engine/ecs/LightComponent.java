package engine.ecs;

import engine.graphics.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@ComponentArray(mask = 1 << 3)
public class LightComponent {
    public Matrix4f view, proj;
    public Matrix4f invView, invProj;
    public boolean invertY;
    public RenderTarget renderTarget;
    public float attenuationConstant = 1f;
    public float attenuationLinear = 0.007f;
    public float attenuationQuadratic = 0.0002f;
    public Vector3f color = new Vector3f(3, 3, 3);
    public float shadowNormalOffsetBias = 0.0001f;
    public float shadowTestOffsetBias = 0.00001f;

    public LightComponent(Disposable parent, Matrix4f view, Matrix4f proj, boolean invertY) {
        setView(view);
        setProj(proj);
        this.invertY = invertY;

        renderTarget = new RenderTarget(parent);

        renderTarget.addAttachment(
                new RenderTargetAttachment(
                        RenderTargetAttachmentTypes.Depth,
                        new Texture[]{
                                Texture.newDepthTexture(renderTarget, 1024, 1024, TextureFormatType.Depth32),
                                Texture.newDepthTexture(renderTarget, 1024, 1024, TextureFormatType.Depth32)
                        },
                        new Sampler[]{
                                Sampler.newSampler(renderTarget, Texture.Filter.Nearest, Texture.Filter.Nearest, false),
                                Sampler.newSampler(renderTarget, Texture.Filter.Nearest, Texture.Filter.Nearest, false)
                        }
                )
        );

        if(invertY){
            this.proj.m11(this.proj.m11() * -1);
        }
    }

    public Matrix4f getProj() {
        return proj;
    }

    public void setProj(Matrix4f proj) {
        this.proj = proj;
        this.invProj = new Matrix4f(proj).invert();
    }

    public void setView(Matrix4f view) {
        this.view = view;
        this.invView = new Matrix4f(view).invert();
    }


}
