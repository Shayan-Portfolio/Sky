package engine.graphics.vulkan;

import engine.logging.SkyRuntimeException;
import engine.graphics.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanComputePass extends ComputePass {

    private VkQueue computeQueue;
    private VkCommandBuffer[] commandBuffers;
    private VkDevice device;
    private VulkanCommandPool commandPool;

    public VulkanComputePass(Disposable parent, String name, int framesInFlight) {
        super(parent, name, framesInFlight);

        computeQueue = VulkanRuntime.getComputeQueue();
        device = VulkanRuntime.getCurrentDevice();

        commandPool = new VulkanCommandPool(this, device, VulkanRuntime.getComputeFamilyIndex());



        finishedSemaphores = new VulkanSemaphore[framesInFlight];
        commandBuffers = new VkCommandBuffer[framesInFlight];

        for (int i = 0; i < framesInFlight; i++) {
            finishedSemaphores[i] = new VulkanSemaphore(this, device);


            try(MemoryStack stack = stackPush()) {
                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(commandPool.getHandle());
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(framesInFlight);

                PointerBuffer pCommandBuffers = stack.mallocPointer(framesInFlight);

                if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                    throw new SkyRuntimeException("Failed to allocate command buffers");
                }

                commandBuffers[i] = new VkCommandBuffer(pCommandBuffers.get(i), device);
            }
        }
    }

    @Override
    public void setPushConstants(ByteBuffer pPushConstants) {
        pPushConstants.rewind();
        long pipelineLayoutHandle = ((VulkanShaderProgram) shaderProgram).getPipeline().getLayoutHandle();
        vkCmdPushConstants(commandBuffers[frameIndex], pipelineLayoutHandle, VK_SHADER_STAGE_ALL, 0, pPushConstants);
    }


    @Override
    public void setShaderProgram(ShaderProgram shaderProgram) {
        try(MemoryStack stack = stackPush()) {
            this.shaderProgram = shaderProgram;
            VulkanPipeline pipeline = ((VulkanShaderProgram) shaderProgram).getPipeline();
            vkCmdBindPipeline(commandBuffers[frameIndex], VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.getHandle());
            vkCmdBindDescriptorSets(commandBuffers[frameIndex], VK_PIPELINE_BIND_POINT_COMPUTE,
                    pipeline.getLayoutHandle(), 0, stack.longs(((VulkanShaderProgram) shaderProgram).getDescriptorSetsHandles(frameIndex)), null);
        }
    }

    @Override
    public void startRecording(int frameIndex) {
        super.startRecording(frameIndex);

        vkResetCommandBuffer(commandBuffers[frameIndex], 0);

        try(MemoryStack stack = stackPush()) {


            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (vkBeginCommandBuffer(commandBuffers[frameIndex], beginInfo) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to begin recording command buffer");
            }
        }
    }

    @Override
    public void dispatch(int workGroupCountX, int workGroupCountY, int workGroupCountZ) {
        vkCmdDispatch(commandBuffers[frameIndex], workGroupCountX, workGroupCountY, workGroupCountZ);
    }


    @Override
    public void endRecording() {
        if (vkEndCommandBuffer(commandBuffers[frameIndex]) != VK_SUCCESS) {
            throw new SkyRuntimeException("Failed to record command buffer");
        }
    }

    @Override
    public void waitForFinish() {
        vkQueueWaitIdle(computeQueue);
    }

    @Override
    public void resolveStartingBarriers() {
        startingBarriers.run(commandBuffers[frameIndex]);
    }

    @Override
    public void resolveEndingBarriers() {
        endingBarriers.run(commandBuffers[frameIndex]);
    }


    @Override
    public void dispose() {}
}
