package engine.graphics.vulkan;

import engine.logging.SkyRuntimeException;
import engine.graphics.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_OBJECT_NAME_INFO_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.vkSetDebugUtilsObjectNameEXT;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanUtil {
    public static final int UINT64_MAX = 0xFFFFFFFF;


    public static int getVulkanImageFormat(TextureFormatType textureFormatType) {
        switch (textureFormatType) {
            case ColorR8G8B8A8 -> {
                return VK_FORMAT_R8G8B8A8_SRGB;
            }
            case ColorR32G32B32A32 -> {
                return VK_FORMAT_R32G32B32A32_SFLOAT;
            }
            case ColorR16G16B16A16 -> {
                return VK_FORMAT_R16G16B16A16_SFLOAT;
            }
            case Depth32 -> {
                return VK_FORMAT_D32_SFLOAT;
            }
            case ColorR8G8B8A8unorm -> {
                return VK_FORMAT_R8G8B8A8_UNORM;
            }
        }
        return -1;
    }

    public static int getVulkanDepthTestType(DepthTestType depthTestType) {
        if(depthTestType == null) return -1;
        switch(depthTestType) {
            case LessThan -> {
                return VK_COMPARE_OP_LESS;
            }
            case GreaterThan -> {
                return VK_COMPARE_OP_GREATER;
            }
            case LessOrEqualTo -> {
                return VK_COMPARE_OP_LESS_OR_EQUAL;
            }
            case GreaterOrEqualTo -> {
                return VK_COMPARE_OP_GREATER_OR_EQUAL;
            }
            case Always -> {
                return VK_COMPARE_OP_ALWAYS;
            }
            case Never -> {
                return VK_COMPARE_OP_NEVER;
            }
        }

        throw new SkyRuntimeException("The depth operation for this pipeline is an invalid value [" + depthTestType + "]");
    }

    public static void transitionImages(VulkanImage image,
                                        VkCommandBuffer commandBuffer,
                                        int newLayout,
                                        int srcAccessMask,
                                        int dstAccessMask,
                                        int aspectMask,
                                        int srcStageMask,
                                        int dstStageMask,
                                        int layerCount) {
        if(image.getCurrentLayout() == newLayout) return;


        try(MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer imageBarrier = VkImageMemoryBarrier.calloc(1, stack);
            {
                imageBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                imageBarrier.oldLayout(image.getCurrentLayout());
                imageBarrier.newLayout(newLayout);
                imageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                imageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                imageBarrier.srcAccessMask(srcAccessMask);
                imageBarrier.dstAccessMask(dstAccessMask);
                imageBarrier.image(image.getHandle());
                imageBarrier.subresourceRange().aspectMask(aspectMask);
                imageBarrier.subresourceRange().baseMipLevel(0);
                imageBarrier.subresourceRange().levelCount(1);
                imageBarrier.subresourceRange().baseArrayLayer(0);
                imageBarrier.subresourceRange().layerCount(layerCount);
            }

            if(image.getFormat() == VK_FORMAT_D32_SFLOAT && aspectMask == VK_IMAGE_ASPECT_COLOR_BIT) {
                System.out.println();
            }



            vkCmdPipelineBarrier(
                    commandBuffer,
                    srcStageMask,
                    dstStageMask,
                    0,
                    null,
                    null,
                    imageBarrier
            );



            image.setCurrentLayout(newLayout);
        }
    }

    public static void nameObject(String name, int type, long handle, MemoryStack stack) {
        if(VulkanRuntime.hasValidation()){
            VkDebugUtilsObjectNameInfoEXT debugUtilsObjectNameInfoEXT = VkDebugUtilsObjectNameInfoEXT.calloc(stack);
            debugUtilsObjectNameInfoEXT.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_OBJECT_NAME_INFO_EXT);
            debugUtilsObjectNameInfoEXT.objectType(type);
            debugUtilsObjectNameInfoEXT.pObjectName(stack.UTF8(name));
            debugUtilsObjectNameInfoEXT.objectHandle(handle);
            vkSetDebugUtilsObjectNameEXT(VulkanRuntime.getCurrentDevice(), debugUtilsObjectNameInfoEXT);
        }
    }

    public static int getVulkanShaderStage(ShaderType shaderType) {
        int shaderStage = 0;

        switch(shaderType) {
            case VertexShader -> {
                shaderStage = VK_SHADER_STAGE_VERTEX_BIT;
            }
            case FragmentShader -> {
                shaderStage = VK_SHADER_STAGE_FRAGMENT_BIT;
            }
            case ComputeShader -> {
                shaderStage = VK_SHADER_STAGE_COMPUTE_BIT;
            }
        }

        return shaderStage;
    }

    public static int getVulkanVertexAttributeType(int size) {
        switch(size){
            case 1: return VK_FORMAT_R32_SFLOAT;
            case 2: return VK_FORMAT_R32G32_SFLOAT;
            case 3: return VK_FORMAT_R32G32B32_SFLOAT;
            case 4: return VK_FORMAT_R32G32B32A32_SFLOAT;
        }
        return 0;
    };

    public static int getVulkanDescriptorType(Descriptor.Type type) {

        switch (type) {
            case UniformBuffer -> {
                return VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
            }
            case ShaderStorageBuffer -> {
                return VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
            }
            case CombinedTextureSampler -> {
                return VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
            }
            case SeparateTexture -> {
                return VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE;
            }
            case SeparateSampler -> {
                return VK_DESCRIPTOR_TYPE_SAMPLER;
            }
            case StorageTexture -> {
                return VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
            }
        }

        return 0;
    }

    public static int getVulkanCullMode(CullMode cullMode) {
        switch (cullMode) {
            case Front: return VK_CULL_MODE_FRONT_BIT;
            case Back: return VK_CULL_MODE_BACK_BIT;
        }

        return -1;
    }
}
