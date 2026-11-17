package engine.graphics.vulkan;

import engine.logging.Logger;
import engine.logging.SkyRuntimeException;
import engine.wsi.Surface;
import engine.graphics.GraphicsAPIs;
import engine.graphics.RenderContext;

import engine.graphics.GraphicsOptions;
import org.lwjgl.vulkan.*;

import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;

public class VulkanRenderContext extends RenderContext {

    private long vkSurface;
    private VkInstance instance;
    private Surface surface;


    public VulkanRenderContext() {
    }



    @Override
    public void readyDisplay(Surface surface, GraphicsOptions settings) {
        this.surface = surface;
        if(!surface.supportsRenderAPI(GraphicsAPIs.Vulkan)) {
            throw new SkyRuntimeException(Logger.error(VulkanRenderContext.class, "The surface does not support Vulkan"));
        }
        surface.requestRenderAPI(GraphicsAPIs.Vulkan, settings);

        instance = surface.getVulkanInstance();
        vkSurface = surface.getVulkanSurface();
    }

    public long getDebugMessenger() {
        return surface.getVulkanDebugMessenger();
    }



    public long getVkSurface() {
        return vkSurface;
    }
    public VkInstance getInstance() {
        return instance;
    }
}
