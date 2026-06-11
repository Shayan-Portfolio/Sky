package engine.graphics.vulkan;

import engine.graphics.Disposable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocationInfo;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.util.vma.Vma.vmaCreateImage;
import static org.lwjgl.util.vma.Vma.vmaDestroyImage;
import static org.lwjgl.vulkan.VK13.*;


public class VulkanImage extends Disposable {

    private long handle;
    private LongBuffer pImage;

    private VkImageCreateInfo imageCreateInfo;
    private VkExtent3D extent;
    private long memory;
    private PointerBuffer pAllocation;
    private VmaAllocationInfo allocationInfo;
    private VmaAllocationCreateInfo allocationCreateInfo;
    private int format;
    private VulkanAllocator allocator;
    private int currentLayout;
    private int usage;

    public VulkanImage(Disposable parent, long imageHandle, int currentLayout, int usage, int imageFormat) {
        super(parent);
        this.handle = imageHandle;
        this.currentLayout = currentLayout;
        this.usage = usage;
        this.format = imageFormat;
    }

    public VulkanImage(Disposable parent, VulkanAllocator allocator, int arrayLayers, int width, int height, int format, int usage, int tiling){
        super(parent);
        this.format = format;
        this.allocator = allocator;
        this.currentLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        this.usage = usage;

        try(MemoryStack stack = MemoryStack.stackPush()) {

            if(arrayLayers == 6) {
                extent = VkExtent3D.calloc(stack).set(width, width, 1);
            }
            else
                extent = VkExtent3D.calloc(stack).set(width, height, 1);

            imageCreateInfo = VkImageCreateInfo.calloc(stack);
            imageCreateInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageCreateInfo.imageType(VK_IMAGE_TYPE_2D);
            imageCreateInfo.extent(extent);
            imageCreateInfo.mipLevels(1);
            imageCreateInfo.arrayLayers(arrayLayers);
            imageCreateInfo.format(format);
            imageCreateInfo.tiling(tiling);
            imageCreateInfo.usage(usage);
            imageCreateInfo.initialLayout(currentLayout);
            imageCreateInfo.samples(VK_SAMPLE_COUNT_1_BIT);
            imageCreateInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            if(arrayLayers == 6)
                imageCreateInfo.flags(VK_IMAGE_CREATE_CUBE_COMPATIBLE_BIT);



            allocationCreateInfo = VmaAllocationCreateInfo.calloc(stack);


            pImage = stack.callocLong(1);
            pAllocation = MemoryUtil.memCallocPointer(1);


            allocationInfo = VmaAllocationInfo.calloc(stack);
            vmaCreateImage(allocator.getId(), imageCreateInfo, allocationCreateInfo, pImage, pAllocation, allocationInfo);

            memory = allocationInfo.deviceMemory();
            handle = pImage.get(0);
        }
    }

    public int getCurrentLayout() {
        return currentLayout;
    }

    protected void setCurrentLayout(int currentLayout) {
        this.currentLayout = currentLayout;
    }

    public int getUsage() {
        return usage;
    }

    public int getFormat() {
        return format;
    }

    public long getHandle() {
        return handle;
    }

    @Override
    public void dispose() {
        vkDeviceWaitIdle(VulkanRuntime.getCurrentDevice());
        if(memory != MemoryUtil.NULL) {
            vmaDestroyImage(VulkanAllocator.getAllocator().getId(), handle, pAllocation.get(0));
            pAllocation.free();
        }
    }

}
