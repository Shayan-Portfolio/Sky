package engine.graphics.vulkan;

import engine.logging.SkyRuntimeException;
import engine.graphics.Disposable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanSwapchain extends Disposable {
    private long handle;
    private List<Long> images = new ArrayList<>();
    private int imageFormat;
    private VkExtent2D extent;

    public VulkanSwapchain(Disposable parent, long surface, int minImageCount, int imageFormat, int imageColorSpace, int presentMode, VulkanQueueFamilies physicalDeviceQueueFamilies, VulkanSwapchainSupportInfo swapchainSupportDetails, VkExtent2D extent){
        super(parent);
        this.imageFormat = imageFormat;
        this.extent = extent;



        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack);

            swapchainCreateInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            swapchainCreateInfo.surface(surface);
            swapchainCreateInfo.minImageCount(minImageCount);
            swapchainCreateInfo.imageFormat(imageFormat);
            swapchainCreateInfo.imageColorSpace(imageColorSpace);
            swapchainCreateInfo.imageExtent(extent);
            swapchainCreateInfo.imageArrayLayers(1);
            swapchainCreateInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            if (physicalDeviceQueueFamilies.graphicsFamily != physicalDeviceQueueFamilies.presentFamily) {
                swapchainCreateInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                swapchainCreateInfo.pQueueFamilyIndices(stack.ints(physicalDeviceQueueFamilies.graphicsFamily, physicalDeviceQueueFamilies.presentFamily));
            }
            else {
                swapchainCreateInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            swapchainCreateInfo.preTransform(swapchainSupportDetails.capabilities.currentTransform());
            swapchainCreateInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            swapchainCreateInfo.presentMode(presentMode);
            swapchainCreateInfo.clipped(true);

            swapchainCreateInfo.oldSwapchain(VK_NULL_HANDLE);

            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);

            if (vkCreateSwapchainKHR(VulkanRuntime.getCurrentDevice(), swapchainCreateInfo, null, pSwapChain) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to create swap chain");
            }

            handle = pSwapChain.get(0);
        }

    }

    public long getHandle() {
        return handle;
    }

    public List<Long> getImages() {
        return images;
    }

    public int getImageFormat() {
        return imageFormat;
    }

    public VkExtent2D getExtent() {
        return extent;
    }

    @Override
    public void dispose() {
        extent.free();
        vkDestroySwapchainKHR(VulkanRuntime.getCurrentDevice(), handle, null);
    }
}
