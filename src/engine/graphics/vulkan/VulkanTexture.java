package engine.graphics.vulkan;

import engine.logging.Logger;
import engine.logging.SkyRuntimeException;
import engine.asset.Asset;
import engine.asset.TextureData;
import engine.graphics.Buffer;
import engine.graphics.Disposable;
import engine.graphics.Texture;
import engine.graphics.TextureFormatType;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanTexture extends Texture {

    private VulkanImage image;
    private VulkanImageView imageView;
    private Buffer imageData;
    private VkCommandBuffer commandBuffer;
    private VulkanFence fence;
    private VulkanCommandPool commandPool;


    public VulkanTexture(Disposable parent, int width, int height, long imageHandle, int currentLayout, int usage, int imageFormat, int aspectMask) {
        super(parent, width, height, null, toTextureFormatType(imageFormat));
        image = new VulkanImage(
                this,
                imageHandle,
                currentLayout,
                usage,
                imageFormat
        );
        imageView = new VulkanImageView(image, VulkanRuntime.getCurrentDevice(), image, aspectMask, 1);
        isStorageTexture = (usage & VK_IMAGE_USAGE_STORAGE_BIT) != 0;
    }

    public VulkanTexture(Disposable parent, int width, int height, int arrayLayers, List<Asset<TextureData>> textureData, int imageFormat, int usage, int tiling, int aspectMask) {
        super(parent, width, height, textureData, toTextureFormatType(imageFormat));

        image = new VulkanImage(
                this,
                VulkanAllocator.getAllocator(),
                arrayLayers,
                getWidth(),
                getHeight(),
                imageFormat,
                usage,
                tiling
        );

        isStorageTexture = (usage & VK_IMAGE_USAGE_STORAGE_BIT) != 0;

        imageView = new VulkanImageView(image, VulkanRuntime.getCurrentDevice(), image, aspectMask, arrayLayers);

        if(textureData != null) {
            imageData = Buffer.newBuffer(this, getWidth() * getHeight() * 4 * arrayLayers, Buffer.Usage.ImageBackingBuffer, Buffer.Type.CPUGPUShared, false);
            ByteBuffer bytes = imageData.map();
            for (Asset<TextureData> faceAsset : textureData) {
                bytes.put(faceAsset.getObject().data);
            }
            imageData.unmap();
        }


        try (MemoryStack stack = stackPush()) {


            commandPool = new VulkanCommandPool(this, VulkanRuntime.getCurrentDevice(), VulkanRuntime.getGraphicsFamilyIndex());

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool.getHandle());
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffers = stack.mallocPointer(1);

            if (vkAllocateCommandBuffers(VulkanRuntime.getCurrentDevice(), allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to create command buffer");
            }

            commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), VulkanRuntime.getCurrentDevice());

            fence = new VulkanFence(this, VulkanRuntime.getCurrentDevice(), 0);


            vkResetFences(VulkanRuntime.getCurrentDevice(), fence.getHandle());

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to start recording command buffer");
            }

            if(textureData != null) {

                VulkanUtil.transitionImageLayout(
                        image,
                        commandBuffer,
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        0,
                        VK_ACCESS_TRANSFER_WRITE_BIT,
                        VK_IMAGE_ASPECT_COLOR_BIT,
                        VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        arrayLayers
                );


                VkBufferImageCopy.Buffer imageCopies = VkBufferImageCopy.calloc(1, stack);
                imageCopies.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                imageCopies.imageSubresource().layerCount(arrayLayers);
                imageCopies.imageExtent().set(width, height, 1);


                vkCmdCopyBufferToImage(commandBuffer, ((VulkanBuffer) imageData).getHandle(), image.getHandle(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, imageCopies);


                VulkanUtil.transitionImageLayout(
                        image,
                        commandBuffer,
                        VK_IMAGE_LAYOUT_GENERAL,
                        VK_ACCESS_TRANSFER_WRITE_BIT,
                        VK_ACCESS_SHADER_READ_BIT,
                        aspectMask,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                        arrayLayers
                );
            }
            else {
                VulkanUtil.transitionImageLayout(
                        image,
                        commandBuffer,
                        VK_IMAGE_LAYOUT_GENERAL,
                        VK_ACCESS_TRANSFER_WRITE_BIT,
                        VK_ACCESS_SHADER_READ_BIT,
                        aspectMask,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                        arrayLayers
                );
            }

            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new SkyRuntimeException(Logger.error(VulkanTexture.class, "Failed to finish recording command buffer"));
            }

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(stack.pointers(commandBuffer));

            if (vkQueueSubmit(VulkanRuntime.getGraphicsQueue(), submitInfo, fence.getHandle()) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to submit command buffer");
            }

            vkWaitForFences(VulkanRuntime.getCurrentDevice(), fence.getHandle(), true, VulkanUtil.UINT64_MAX);
        }


    }

    private static TextureFormatType toTextureFormatType(int vulkanImageFormatEnum) {
        switch (vulkanImageFormatEnum) {
            case VK_FORMAT_R8G8B8A8_SRGB -> {
                return TextureFormatType.ColorR8G8B8A8;
            }
            case VK_FORMAT_R16G16B16A16_SFLOAT -> {
                return TextureFormatType.ColorR16G16B16A16;
            }
            case VK_FORMAT_R32G32B32A32_SFLOAT -> {
                return TextureFormatType.ColorR32G32B32A32;
            }
            case VK_FORMAT_D32_SFLOAT -> {
                return TextureFormatType.Depth32;
            }
        }

        return null;
    }



    public VulkanImage getImage() {
        return image;
    }

    public VulkanImageView getImageView() {
        return imageView;
    }

    @Override
    public void dispose() {}

}
