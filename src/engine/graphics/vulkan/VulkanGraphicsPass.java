package engine.graphics.vulkan;

import engine.logging.SkyRuntimeException;
import engine.graphics.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Optional;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK13.vkCmdSetCullMode;

public class VulkanGraphicsPass extends GraphicsPass {

    private VkQueue graphicsQueue;
    private VkCommandBuffer[] commandBuffers;
    private VkDevice device;
    private VkRenderingInfoKHR renderingInfoKHR;
    private VkRenderingAttachmentInfo.Buffer renderingAttachmentInfoKHRs;
    private VkRenderingAttachmentInfoKHR depthRenderingAttachmentInfoKHR;
    private VulkanCommandPool commandPool;


    public VulkanGraphicsPass(Disposable parent, String name, int framesInFlight) {
        super(parent, name, framesInFlight);

        graphicsQueue = VulkanRuntime.getGraphicsQueue();
        device = VulkanRuntime.getCurrentDevice();
        commandPool = new VulkanCommandPool(this, device, VulkanRuntime.getGraphicsFamilyIndex());

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
                VulkanUtil.nameObject(name, VK_OBJECT_TYPE_COMMAND_BUFFER, pCommandBuffers.get(i), stack);
            }
        }

    }

    @Override
    public void setDrawBuffers(Buffer vertexBuffer, Buffer indexBuffer) {
        try(MemoryStack stack = stackPush()) {
            VulkanBuffer vulkanVertexBuffer = (VulkanBuffer) vertexBuffer;
            VulkanBuffer vulkanIndexBuffer = (VulkanBuffer) indexBuffer;

            LongBuffer vertexBuffers = stack.longs(vulkanVertexBuffer.getHandle());
            LongBuffer offsets = stack.longs(0);

            vkCmdBindVertexBuffers(commandBuffers[frameIndex], 0, vertexBuffers, offsets);
            vkCmdBindIndexBuffer(commandBuffers[frameIndex], vulkanIndexBuffer.getHandle(), 0, VK_INDEX_TYPE_UINT32);


        }
    }

    @Override
    public void setShaderProgram(ShaderProgram shaderProgram) {
        try(MemoryStack stack = stackPush()) {
            this.shaderProgram = shaderProgram;
            VulkanPipeline pipeline = ((VulkanShaderProgram) shaderProgram).getPipeline();
            vkCmdBindPipeline(commandBuffers[frameIndex], VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle());
            vkCmdBindDescriptorSets(commandBuffers[frameIndex], VK_PIPELINE_BIND_POINT_GRAPHICS,
                    pipeline.getLayoutHandle(), 0, stack.longs(((VulkanShaderProgram) shaderProgram).getDescriptorSetsHandles(frameIndex)), null);

        }
    }

    @Override
    public void setPushConstants(ByteBuffer pPushConstants) {
        pPushConstants.rewind();
        long pipelineLayoutHandle = ((VulkanShaderProgram) shaderProgram).getPipeline().getLayoutHandle();
        vkCmdPushConstants(commandBuffers[frameIndex], pipelineLayoutHandle, VK_SHADER_STAGE_ALL, 0, pPushConstants);
    }

    @Override
    public void drawInstanced(int indexCount, int instanceCount) {
        vkCmdDrawIndexed(
                commandBuffers[frameIndex],
                indexCount,
                instanceCount,
                0,
                0,
                0
        );
    }

    @Override
    public void drawIndexed(int indexCount) {
        vkCmdDrawIndexed(
                commandBuffers[frameIndex],
                indexCount,
                1,
                0,
                0,
                0
        );
    }



    @Override
    public void startRecording(int frameIndex) {
        super.startRecording(frameIndex);

        try(MemoryStack stack = stackPush()) {

            vkResetCommandBuffer(commandBuffers[frameIndex], 0);
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (vkBeginCommandBuffer(commandBuffers[frameIndex], beginInfo) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to begin recording command buffer");
            }
        }
    }

    @Override
    public void startRendering(RenderTarget renderTarget, int unusedAttachmentCount, int width, int height, boolean clear, Color color) {
        this.renderTarget = renderTarget;

        int loadOp = VK_ATTACHMENT_LOAD_OP_LOAD;
        if(clear) loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;


        try(MemoryStack stack = stackPush()) {

            VkClearValue colorClearValue = VkClearValue.calloc(stack);
            colorClearValue.color().float32(stack.floats(color.r, color.g, color.b, color.a));



            int attachmentCount = renderTarget.getAttachmentCountExcludingDepth() + unusedAttachmentCount;



            renderingAttachmentInfoKHRs = VkRenderingAttachmentInfoKHR.calloc(attachmentCount, stack);
            {
                for (int i = 0; i < attachmentCount; i++) {

                    VkRenderingAttachmentInfoKHR renderingAttachmentInfoKHR = (VkRenderingAttachmentInfoKHR) renderingAttachmentInfoKHRs.get(i);
                    renderingAttachmentInfoKHR.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);

                    if(i > renderTarget.getAttachmentCountExcludingDepth() - 1) {
                        renderingAttachmentInfoKHR.imageView(VK_NULL_HANDLE);
                        renderingAttachmentInfoKHR.imageLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                        renderingAttachmentInfoKHR.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
                        renderingAttachmentInfoKHR.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
                    }
                    else {

                        VulkanTexture texture = (VulkanTexture) renderTarget.getAttachmentByIndex(i).getTextures()[frameIndex];

                        renderingAttachmentInfoKHR.imageView(texture.getImageView().getHandle());
                        renderingAttachmentInfoKHR.imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                        renderingAttachmentInfoKHR.loadOp(loadOp);
                        renderingAttachmentInfoKHR.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
                        renderingAttachmentInfoKHR.clearValue(colorClearValue);
                    }

                }





            }

            VkClearValue depthClearValue = VkClearValue.calloc(stack);
            depthClearValue.depthStencil().set(1.0f, 0);
            Attachment depthAttachment = renderTarget.getAttachment(AttachmentTypes.Depth);



            depthRenderingAttachmentInfoKHR = VkRenderingAttachmentInfoKHR.calloc(stack);
            {


                VulkanTexture depthImage = ((VulkanTexture) depthAttachment.getTextures()[frameIndex]);
                VulkanImageView depthImageView = depthImage.getImageView();

                depthRenderingAttachmentInfoKHR.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
                depthRenderingAttachmentInfoKHR.imageView(depthImageView.getHandle());
                depthRenderingAttachmentInfoKHR.imageLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
                depthRenderingAttachmentInfoKHR.loadOp(loadOp);
                depthRenderingAttachmentInfoKHR.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
                depthRenderingAttachmentInfoKHR.clearValue(depthClearValue);
            }

            renderingInfoKHR = VkRenderingInfoKHR.calloc(stack);
            renderingInfoKHR.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_INFO_KHR);
            VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
            renderArea.extent(VkExtent2D.calloc(stack).set(width, height));
            renderingInfoKHR.renderArea(renderArea);
            renderingInfoKHR.layerCount(1);
            renderingInfoKHR.pColorAttachments(renderingAttachmentInfoKHRs);
            renderingInfoKHR.pDepthAttachment(depthRenderingAttachmentInfoKHR);


            //System.out.println("[" + name + "]");
            KHRDynamicRendering.vkCmdBeginRenderingKHR(commandBuffers[frameIndex], renderingInfoKHR);
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width(width);
            viewport.height(height);




            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);


            vkCmdSetViewport(commandBuffers[frameIndex], 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            {
                scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
                scissor.extent(VkExtent2D.calloc(stack).set(width, height));
            }

            vkCmdSetScissor(commandBuffers[frameIndex], 0, scissor);

        }
    }

    @Override
    public void endRendering() {
        KHRDynamicRendering.vkCmdEndRenderingKHR(commandBuffers[frameIndex]);
    }

    @Override
    public void setCullMode(CullMode cullMode) {
        vkCmdSetCullMode(commandBuffers[frameIndex], VulkanUtil.getVulkanCullMode(cullMode));
    }


    @Override
    public void endRecording() {
        if (vkEndCommandBuffer(commandBuffers[frameIndex]) != VK_SUCCESS) {
            throw new SkyRuntimeException("Failed to record command buffer");
        }
    }

    @Override
    public void submit(Optional<Fence[]> submissionFences) {
        try(MemoryStack stack = stackPush()) {

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.waitSemaphoreCount(1);

            submitInfo.pWaitSemaphores(stack.longs(((VulkanSemaphore) waitSemaphores[frameIndex]).getHandle()));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(commandBuffers[frameIndex]));
            submitInfo.pSignalSemaphores(stack.longs(((VulkanSemaphore) finishedSemaphores[frameIndex]).getHandle()));

            if(submissionFences.isPresent())
                vkQueueSubmit(graphicsQueue, submitInfo, ((VulkanFence[]) submissionFences.get())[frameIndex].getHandle());
            else vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE);
        }


    }

    @Override
    public void waitForFinish() {
        vkQueueWaitIdle(graphicsQueue);
    }

    @Override
    public void resolveBarriers() {
        barrierCallback.run(commandBuffers[frameIndex]);
    }


    @Override
    public void dispose() {
        vkDeviceWaitIdle(VulkanRuntime.getCurrentDevice());
    }
}
