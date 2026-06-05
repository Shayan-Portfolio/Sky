package engine.ecs;

import engine.graphics.*;

@ComponentArray(mask = 1 << 3)
public class LightComponent {

    public LightData data;

    public LightComponent(Disposable parent, LightData data) {
        this.data = data;
        this.data.renderTarget = new RenderTarget(parent);


        this.data.renderTarget.addAttachment(
                new Attachment(
                        AttachmentTypes.Depth,
                        new Texture[]{
                                Texture.newDepthTexture(this.data.renderTarget, 1024, 1024, TextureFormatType.Depth32),
                                Texture.newDepthTexture(this.data.renderTarget, 1024, 1024, TextureFormatType.Depth32)
                        },
                        new Sampler[]{
                                Sampler.newSampler(this.data.renderTarget, Texture.Filter.Nearest, Texture.Filter.Nearest, false),
                                Sampler.newSampler(this.data.renderTarget, Texture.Filter.Nearest, Texture.Filter.Nearest, false)
                        }
                )
        );

        if(this.data.invertY){
            this.data.proj.m11(this.data.proj.m11() * -1);
        }
    }



}
