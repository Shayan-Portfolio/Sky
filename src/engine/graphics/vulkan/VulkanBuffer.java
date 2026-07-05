package engine.graphics.vulkan;

import engine.logging.SkyRuntimeException;
import engine.graphics.Buffer;
import engine.graphics.Disposable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocationInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;



public class VulkanBuffer extends Buffer {

    private long handle;
    private LongBuffer pBuffer;
    private long allocation;
    private VmaAllocationInfo allocationInfo;
    private VmaAllocationCreateInfo allocationCreateInfo;
    private long memory;
    private PointerBuffer mappedMemory;
    private VkCommandBuffer commandBuffer;
    private VulkanCommandPool commandPool;

    public VulkanBuffer(Disposable parent, int sizeBytes, Usage usage, Type type, boolean staging){
        super(parent, sizeBytes, usage, type, staging);


        try(MemoryStack stack = MemoryStack.stackPush()) {

            VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(stack);
            bufferCreateInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferCreateInfo.size(sizeBytes);
            int vkUsage = toVkUsageType(usage);

            if (type == Type.CPUGPUShared && staging) vkUsage |= VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
            if (type == Type.GPULocal) vkUsage |= VK_BUFFER_USAGE_TRANSFER_DST_BIT;

            bufferCreateInfo.usage(vkUsage);


            allocationCreateInfo = VmaAllocationCreateInfo.calloc(stack);
            allocationCreateInfo.usage(toVkMemoryUsageType(type));
            allocationCreateInfo.requiredFlags(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            pBuffer = stack.callocLong(1);
            PointerBuffer pAllocation = stack.callocPointer(1);

            allocationInfo = VmaAllocationInfo.calloc(stack);

            vmaCreateBuffer(VulkanAllocator.getAllocator().getId(), bufferCreateInfo, allocationCreateInfo, pBuffer, pAllocation, allocationInfo);
            allocation = pAllocation.get(0);
            memory = allocationInfo.deviceMemory();

            handle = pBuffer.get();

            //Resources for copying data between buffers
            {
                commandPool = new VulkanCommandPool(this, VulkanRuntime.getCurrentDevice(), VulkanRuntime.getGraphicsFamilyIndex());


                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(commandPool.getHandle());
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(1);


                PointerBuffer pCommandBuffers = stack.mallocPointer(1);

                if(vkAllocateCommandBuffers(VulkanRuntime.getCurrentDevice(), allocInfo, pCommandBuffers) != VK_SUCCESS) {
                    throw new SkyRuntimeException("Failed to create command buffers");
                }

                commandBuffer = new VkCommandBuffer(pCommandBuffers.get(0), VulkanRuntime.getCurrentDevice());

                copyFence = new VulkanFence(this, VulkanRuntime.getCurrentDevice(), 0);
            }
        }

    }

    private int toVkUsageType(Usage usage){
        switch(usage){
            case VertexBuffer -> {
                return VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
            }
            case IndexBuffer -> {
                return VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
            }
            case ShaderStorageBuffer -> {
                return VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
            }
            case UniformBuffer -> {
                return VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
            }
            case ImageBackingBuffer -> {
                return VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
            }
        }

        return 0;
    }

    private int toVkMemoryUsageType(Type type) {
        switch (type) {
            case GPULocal -> {
                return VMA_MEMORY_USAGE_GPU_ONLY;
            }
            case CPUGPUShared -> {
                return VMA_MEMORY_USAGE_CPU_TO_GPU;
            }
        }

        return 0;
    }


    public ByteBuffer map(){
        super.map();
        mappedMemory = MemoryUtil.memCallocPointer(1);
        vmaMapMemory(VulkanAllocator.getAllocator().getId(), allocation, mappedMemory);
        return mappedMemory.getByteBuffer(getSizeBytes());
    }


    @Override
    public void copyTo(Buffer dst, int srcOffset, int dstOffset, int size) {
        vkResetFences(VulkanRuntime.getCurrentDevice(), ((VulkanFence) copyFence).getHandle());

        try (MemoryStack stack = stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to start recording copy command buffer");
            }


            VkBufferCopy.Buffer copyInfo = VkBufferCopy.calloc(1, stack);
            copyInfo.size(size);
            copyInfo.srcOffset(srcOffset);
            copyInfo.dstOffset(dstOffset);

            vkCmdCopyBuffer(commandBuffer, this.getHandle(), ((VulkanBuffer) dst).getHandle(), copyInfo);

            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to stop recording copy command buffer");
            }

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(stack.pointers(commandBuffer));

            if (vkQueueSubmit(VulkanRuntime.getGraphicsQueue(), submitInfo, ((VulkanFence) copyFence).getHandle()) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to submit command buffer to queue");
            }

            vkWaitForFences(VulkanRuntime.getCurrentDevice(), ((VulkanFence) copyFence).getHandle(), true, VulkanUtil.UINT64_MAX);

        }
    }


    public void unmap(){
        super.unmap();

        vmaUnmapMemory(VulkanAllocator.getAllocator().getId(), allocation);
        MemoryUtil.memFree(mappedMemory);
    }

    public long getMemory() {
        return memory;
    }

    public long getHandle() {
        return handle;
    }

    @Override
    public void dispose() {


        if(mapped) unmap();
        vkDeviceWaitIdle(VulkanRuntime.getCurrentDevice());
        vmaDestroyBuffer(VulkanAllocator.getAllocator().getId(), handle, allocation);
    }

}
