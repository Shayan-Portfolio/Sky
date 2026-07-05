package engine.graphics.vulkan;

import engine.logging.Logger;
import engine.logging.SkyRuntimeException;
import engine.graphics.Disposable;
import engine.graphics.Sampler;
import engine.graphics.Texture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;

import static engine.graphics.Texture.Filter.Linear;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanSampler extends Sampler {
    private long handle;

    public VulkanSampler(Disposable parent, Texture.Filter minFilter, Texture.Filter magFilter, boolean anisotropy) {
        super(parent, minFilter, magFilter, anisotropy);

        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerCreateInfo = VkSamplerCreateInfo.calloc(stack);
            samplerCreateInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            samplerCreateInfo.minFilter(minFilter == Linear ? VK_FILTER_LINEAR : VK_FILTER_NEAREST);
            samplerCreateInfo.magFilter(magFilter == Linear ? VK_FILTER_LINEAR : VK_FILTER_NEAREST);
            samplerCreateInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerCreateInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerCreateInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerCreateInfo.anisotropyEnable(anisotropy);
            samplerCreateInfo.maxAnisotropy(VulkanRuntime.getPhysicalDeviceProperties().limits().maxSamplerAnisotropy());
            samplerCreateInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
            samplerCreateInfo.unnormalizedCoordinates(false);
            samplerCreateInfo.compareEnable(false);
            samplerCreateInfo.compareOp(VK_COMPARE_OP_ALWAYS);
            samplerCreateInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);

            LongBuffer pSampler = stack.callocLong(1);
            if (vkCreateSampler(VulkanRuntime.getCurrentDevice(), samplerCreateInfo, null, pSampler) != VK_SUCCESS) {
                throw new SkyRuntimeException(Logger.error(VulkanSampler.class, "Failed to create sampler!"));
            }

            handle = pSampler.get(0);

        }


    }
    public long getHandle() {
        return handle;
    }

    @Override
    public void dispose() {
        vkDeviceWaitIdle(VulkanRuntime.getCurrentDevice());
        vkDestroySampler(VulkanRuntime.getCurrentDevice(), handle, null);
    }

}
