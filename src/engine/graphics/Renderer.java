package engine.graphics;

import engine.logging.Logger;
import engine.Surface;
import engine.graphics.vulkan.VulkanRenderContext;
import engine.graphics.vulkan.VulkanRenderer;
import org.lwjgl.vulkan.VkInstance;

import java.util.List;

public abstract class Renderer extends Disposable {

    protected int width;
    protected int height;
    private static RenderAPI api;
    protected RendererSettings settings;
    protected int maxFramesInFlight;
    protected Surface surface;
    protected RenderTarget swapchainRenderTarget;
    protected Semaphore[] swapchainImageAcquireSemaphores;
    protected int frameIndex;




    public Renderer(Disposable parent, int width, int height, int maxFramesInFlight, RendererSettings settings, Surface surface){
        super(parent);
        this.width = width;
        this.height = height;
        this.settings = settings;
        this.maxFramesInFlight = maxFramesInFlight;
        this.surface = surface;

    }

    public RendererSettings getSettings() {
        return settings;
    }


    public abstract void updateRenderer(boolean surfaceInvalidated);
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
    public static Renderer newRenderer(Surface surface, int width, int height, RendererSettings settings){
        api = settings.backend;


        if(settings.backend == RenderAPI.Vulkan){
            VulkanRenderContext vkContext = new VulkanRenderContext();
            vkContext.readyDisplay(surface, settings);

            long vkSurface = vkContext.getVkSurface();
            VkInstance instance = vkContext.getInstance();

            return new VulkanRenderer(surface, instance, vkSurface, width, height, settings, vkContext.getDebugMessenger(), surface);
        }
        else if(settings.backend == null){
            Logger.meltdown(Renderer.class, "The target graphics API was not specified in RenderSettings!");
        }
        else {
            Logger.meltdown(Renderer.class, "User requested renderer backend " + settings.backend + " but no backend could be initialized!");
        }



        return null;
    }

    public static RenderAPI getRenderAPI() {
        return api;
    }
    public RenderTarget getSwapchainRenderTarget() { return swapchainRenderTarget; }

    public abstract void submit(List<Pass> passes);
}
