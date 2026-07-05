package engine.graphics.vulkan;

import engine.logging.SkyRuntimeException;
import engine.graphics.Disposable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanImageView extends Disposable {
    private VulkanImage image;
    private long handle;
    private int aspectMask;


    public VulkanImageView(Disposable parent, VkDevice device, VulkanImage image, int aspectMask, int arrayLayers) {
        super(parent);
        this.image = image;
        this.aspectMask = aspectMask;

        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.calloc(stack);
            LongBuffer pImageView = stack.callocLong(1);

            imageViewCreateInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            imageViewCreateInfo.image(image.getHandle());
            int viewType = VK_IMAGE_VIEW_TYPE_2D;
            if(arrayLayers == 6) viewType = VK_IMAGE_VIEW_TYPE_CUBE;

            imageViewCreateInfo.viewType(viewType);
            imageViewCreateInfo.format(image.getFormat());

            imageViewCreateInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
            imageViewCreateInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
            imageViewCreateInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
            imageViewCreateInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);

            imageViewCreateInfo.subresourceRange().aspectMask(aspectMask);
            imageViewCreateInfo.subresourceRange().baseMipLevel(0);
            imageViewCreateInfo.subresourceRange().levelCount(1);
            imageViewCreateInfo.subresourceRange().baseArrayLayer(0);
            imageViewCreateInfo.subresourceRange().layerCount(arrayLayers);

            if (vkCreateImageView(device, imageViewCreateInfo, null, pImageView) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to create image views");
            }

            handle = pImageView.get(0);
        }


    }

    public long getHandle() {
        return handle;
    }

    @Override
    public void dispose() {
        vkDeviceWaitIdle(VulkanRuntime.getCurrentDevice());
        vkDestroyImageView(VulkanRuntime.getCurrentDevice(), handle, null);
    }

    public int getAspectMask() {
        return aspectMask;
    }
}
