package engine.graphics.vulkan;

import engine.logging.SkyRuntimeException;
import engine.graphics.Disposable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class VulkanCommandPool extends Disposable {

    private long handle;
    private VkDevice device;

    public VulkanCommandPool(Disposable parent, VkDevice device, int queueFamilyIndex) {
        super(parent);
        this.device = device;

        try(MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(stack);
            commandPoolCreateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            commandPoolCreateInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
            commandPoolCreateInfo.queueFamilyIndex(queueFamilyIndex);

            LongBuffer pCommandPool = stack.mallocLong(1);

            if(vkCreateCommandPool(device, commandPoolCreateInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to create command pool");
            }
            handle = pCommandPool.get(0);
        }
    }

    public long getHandle() {
        return handle;
    }

    @Override
    public void dispose() {
        vkDeviceWaitIdle(device);
        vkDestroyCommandPool(device, handle, null);
    }
}
