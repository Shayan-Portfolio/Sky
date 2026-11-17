package engine.graphics;

import engine.graphics.vulkan.VulkanSampler;

public abstract class Sampler extends Disposable {
    private Texture.Filter minFilter, magFilter;
    private boolean anisotropy;

    public Sampler(Disposable parent, Texture.Filter minFilter, Texture.Filter magFilter, boolean anisotropy) {
        super(parent);
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.anisotropy = anisotropy;
    }

    public static Sampler newSampler(Disposable parent, Texture.Filter minFilter, Texture.Filter magFilter, boolean anisotropy){
        if(SceneRenderer.getAPI() == GraphicsAPIs.Vulkan){
            return new VulkanSampler(parent, minFilter, magFilter, anisotropy);
        }
        return null;
    }
}
