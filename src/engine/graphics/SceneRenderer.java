package engine.graphics;

import engine.logging.Logger;
import engine.wsi.Surface;
import engine.graphics.vulkan.VulkanRenderContext;
import engine.graphics.vulkan.VulkanSceneRenderer;
import org.lwjgl.vulkan.VkInstance;

import java.util.List;

public abstract class SceneRenderer extends Disposable {

    protected int width;
    protected int height;
    private static GraphicsAPIs api;
    protected GraphicsOptions options;
    protected int maxFramesInFlight;
    protected Surface surface;
    protected RenderTarget swapchainRenderTarget;
    protected Semaphore[] swapchainImageAcquireSemaphores;
    protected int frameIndex;




    public SceneRenderer(Disposable parent, int width, int height, int maxFramesInFlight, GraphicsOptions options, Surface surface){
        super(parent);
        this.width = width;
        this.height = height;
        this.options = options;
        this.maxFramesInFlight = maxFramesInFlight;
        this.surface = surface;

    }

    public GraphicsOptions getOptions() {
        return options;
    }


    public abstract void update(boolean surfaceInvalidated);
    public Semaphore[] getRenderStartSemaphores() {
        return swapchainImageAcquireSemaphores;
    }


    public int getFrameIndex() { return frameIndex; }
    public int getMaxFramesInFlight() { return maxFramesInFlight; }
    public abstract String getDeviceName();

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    public abstract void waitForDevice();
    public static SceneRenderer newSceneRenderer(Surface surface, int width, int height, GraphicsOptions settings){
        api = settings.backend;


        if(settings.backend == GraphicsAPIs.Vulkan){
            VulkanRenderContext vkContext = new VulkanRenderContext();
            vkContext.readyDisplay(surface, settings);

            long vkSurface = vkContext.getVkSurface();
            VkInstance instance = vkContext.getInstance();

            return new VulkanSceneRenderer(surface, instance, vkSurface, width, height, settings, vkContext.getDebugMessenger(), surface);
        }
        else if(settings.backend == null){
            Logger.meltdown(SceneRenderer.class, "The target graphics API was not specified in RenderSettings!");
        }
        else {
            Logger.meltdown(SceneRenderer.class, "User requested renderer backend " + settings.backend + " but no backend could be initialized!");
        }



        return null;
    }

    public static GraphicsAPIs getAPI() {
        return api;
    }
    public RenderTarget getSwapchainRenderTarget() { return swapchainRenderTarget; }

    public abstract void submit(List<Pass> passes);
}
