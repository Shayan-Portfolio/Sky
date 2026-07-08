package engine.graphics.vulkan;

import engine.logging.Logger;
import engine.logging.SkyRuntimeException;
import engine.wsi.Surface;
import engine.graphics.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static engine.graphics.vulkan.VulkanUtil.UINT64_MAX;
import static java.lang.Math.clamp;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.vmaDestroyAllocator;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK13.*;


public class VulkanRenderer extends Renderer {

    public static final int FRAMES_IN_FLIGHT = 2;

    private long debugMessenger;
    private VkPhysicalDevice physicalDevice;
    private VkPhysicalDeviceProperties physicalDeviceProperties;
    private VulkanQueueFamilies queueFamilies;
    private static final Set<String> DEVICE_EXTENSIONS = Stream.of(
                    VK_KHR_SWAPCHAIN_EXTENSION_NAME,
                    KHRDynamicRendering.VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME,
                    KHRComputeShaderDerivatives.VK_KHR_COMPUTE_SHADER_DERIVATIVES_EXTENSION_NAME,
                    EXTDynamicRenderingUnusedAttachments.VK_EXT_DYNAMIC_RENDERING_UNUSED_ATTACHMENTS_EXTENSION_NAME,
                    EXTDescriptorIndexing.VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME)
            .collect(toSet());

    private VkDevice device;
    private VkQueue graphicsQueue, computeQueue, presentQueue;
    private VulkanSwapchain swapchain;
    private VulkanSwapchainSupportInfo swapchainSupportDetails;

    private long vkSurface;
    private VkInstance instance;

    private VulkanAllocator allocator;
    private VulkanFence[] submissionFences;


    public VulkanRenderer(Disposable parent, VkInstance instance, long vkSurface, int width, int height, RendererSettings rendererSettings, long debugMessenger, Surface surface) {
        super(parent, width, height, FRAMES_IN_FLIGHT, rendererSettings, surface);
        this.instance = instance;
        this.vkSurface = vkSurface;
        this.debugMessenger = debugMessenger;

        physicalDevice = selectPhysicalDevice(instance, vkSurface);
        physicalDeviceProperties = getPhysicalDeviceProperties(physicalDevice);

        VulkanRuntime.setValidation(rendererSettings.validation);

        VulkanRuntime.setPhysicalDeviceProperties(physicalDeviceProperties);
        Logger.info(VulkanRenderer.class, "Selected Physical Device " + physicalDeviceProperties.deviceNameString());
        device = createDevice(physicalDevice);

        VulkanRuntime.setCurrentDevice(device);
        VulkanRuntime.setCurrentPhysicalDevice(physicalDevice);
        VulkanAllocator.init(instance, device, physicalDevice);
        allocator = VulkanAllocator.getAllocator();

        graphicsQueue = getGraphicsQueue(device);
        VulkanRuntime.setGraphicsQueue(graphicsQueue);
        computeQueue = getComputeQueue(device);
        VulkanRuntime.setComputeQueue(computeQueue);
        presentQueue = getPresentQueue(device);
        VulkanRuntime.setGraphicsFamilyIndex(queueFamilies.graphicsFamily);
        VulkanRuntime.setComputeFamilyIndex(queueFamilies.computeFamily);

        swapchainRenderTarget = createSwapchainRenderTarget(rendererSettings);

        swapchainImageAcquireSemaphores = new VulkanSemaphore[maxFramesInFlight];
        submissionFences = new VulkanFence[maxFramesInFlight];
        for (int i = 0; i < maxFramesInFlight; i++) {
            submissionFences[i] = new VulkanFence(this, device, VK_FENCE_CREATE_SIGNALED_BIT);
            swapchainImageAcquireSemaphores[i] = new VulkanSemaphore(this, device);
        }

    }


    private RenderTarget createSwapchainRenderTarget(RendererSettings rendererSettings) {
        RenderTarget swapchainRenderTarget = new RenderTarget(this);


        try(MemoryStack stack = stackPush()) {


            //Getting Swapchain Images
            {

                swapchainSupportDetails.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
                vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, vkSurface, swapchainSupportDetails.capabilities);


                VkSurfaceFormatKHR surfaceFormat = selectSwapchainSurfaceFormat(swapchainSupportDetails.formats);
                int presentMode = selectSwapchainPresentMode(swapchainSupportDetails.presentModes, rendererSettings.vsync);
                VkExtent2D extent = selectSwapchainExtent(swapchainSupportDetails.capabilities, width, height);


                IntBuffer imageCount = stack.ints(swapchainSupportDetails.capabilities.minImageCount());


                if (swapchainSupportDetails.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapchainSupportDetails.capabilities.maxImageCount()) {
                    imageCount.put(0, swapchainSupportDetails.capabilities.maxImageCount());
                }


                swapchain = new VulkanSwapchain(
                        this,
                        vkSurface,
                        imageCount.get(0),
                        surfaceFormat.format(),
                        surfaceFormat.colorSpace(),
                        presentMode,
                        queueFamilies,
                        swapchainSupportDetails,
                        extent
                );




                vkGetSwapchainImagesKHR(device, swapchain.getHandle(), imageCount, null);

                LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

                vkGetSwapchainImagesKHR(device, swapchain.getHandle(), imageCount, pSwapchainImages);

                for (int i = 0; i < pSwapchainImages.capacity(); i++) {
                    swapchain.getImages().add(pSwapchainImages.get(i));
                }
            }

        }


        Texture[] colorTextures = new Texture[maxFramesInFlight];
        for (int i = 0; i < swapchain.getImages().size(); i++) {
            colorTextures[i] = new VulkanTexture(
                    swapchainRenderTarget,
                    swapchain.getExtent().width(),
                    swapchain.getExtent().height(),
                    swapchain.getImages().get(i),
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                    swapchain.getImageFormat(),
                    VK_IMAGE_ASPECT_COLOR_BIT
            );
        }

        Attachment colorAttachment = new Attachment(AttachmentTypes.Color0, colorTextures, null);


        Attachment depthAttachment = new Attachment(
                AttachmentTypes.Depth,
                new Texture[] {
                        Texture.newDepthTexture(
                                swapchainRenderTarget,
                                swapchain.getExtent().width(),
                                swapchain.getExtent().height(),
                                TextureFormatType.Depth32
                        ),
                        Texture.newDepthTexture(
                                swapchainRenderTarget,
                                swapchain.getExtent().width(),
                                swapchain.getExtent().height(),
                                TextureFormatType.Depth32
                        )
                },
                null
        );

        swapchainRenderTarget.addAttachment(colorAttachment);
        swapchainRenderTarget.addAttachment(depthAttachment);


        return swapchainRenderTarget;
    }
    private VkPhysicalDevice selectPhysicalDevice(VkInstance instance, long surface){
        try(MemoryStack stack = stackPush()) {

            IntBuffer physicalDevicesCount = stack.ints(0);

            vkEnumeratePhysicalDevices(instance, physicalDevicesCount, null);

            if(physicalDevicesCount.get(0) == 0) {
                throw new SkyRuntimeException("No GPUs on the host support Vulkan");
            }
            PointerBuffer ppPhysicalDevices = stack.mallocPointer(physicalDevicesCount.get(0));
            vkEnumeratePhysicalDevices(instance, physicalDevicesCount, ppPhysicalDevices);

            for(int i = 0; i < ppPhysicalDevices.capacity();i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);


                boolean hasExtensions;
                boolean hasCorrectSwapchain = false;

                //Find the graphics and present queue
                {
                    IntBuffer queueFamilyCount = stack.ints(0);
                    vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);
                    VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
                    vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

                    IntBuffer hasPresentSupport = stack.ints(VK_FALSE);

                    int graphicsFamilyIndex = -1, presentFamilyIndex = -1;

                    for (int queueFamilyIndex = 0; queueFamilyIndex < queueFamilies.capacity(); queueFamilyIndex++) {
                        VkQueueFamilyProperties queueFamilyProperties = queueFamilies.get(queueFamilyIndex);

                        if ((queueFamilyProperties.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0)
                            graphicsFamilyIndex = queueFamilyIndex;

                        vkGetPhysicalDeviceSurfaceSupportKHR(device, queueFamilyIndex, surface, hasPresentSupport);
                        if (hasPresentSupport.get(0) == VK_TRUE) presentFamilyIndex = queueFamilyIndex;
                    }


                    this.queueFamilies = new VulkanQueueFamilies(graphicsFamilyIndex, graphicsFamilyIndex, presentFamilyIndex);
                }

                //Find the requisite extensions
                {
                    IntBuffer extensionCount = stack.ints(0);

                    vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, null);
                    VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);
                    vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, availableExtensions);

                    hasExtensions = availableExtensions.stream()
                            .map(VkExtensionProperties::extensionNameString)
                            .collect(toSet())
                            .containsAll(DEVICE_EXTENSIONS);
                }

                //Find the right swapchain specifications
                if(hasExtensions) {
                    swapchainSupportDetails = new VulkanSwapchainSupportInfo();
                    swapchainSupportDetails.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
                    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, swapchainSupportDetails.capabilities);

                    IntBuffer count = stack.ints(0);

                    vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null);

                    if(count.get(0) != 0) {
                        swapchainSupportDetails.formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
                        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, swapchainSupportDetails.formats);
                    }

                    vkGetPhysicalDeviceSurfacePresentModesKHR(device,surface, count, null);

                    if(count.get(0) != 0) {
                        swapchainSupportDetails.presentModes = stack.mallocInt(count.get(0));
                        vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, swapchainSupportDetails.presentModes);
                    }

                    hasCorrectSwapchain = swapchainSupportDetails.formats.hasRemaining() && swapchainSupportDetails.presentModes.hasRemaining();
                }

                if(queueFamilies.graphicsFamily != -1 &&
                        queueFamilies.presentFamily != -1 &&
                        hasExtensions &&
                        hasCorrectSwapchain){

                    return device;
                }

            }

            throw new SkyRuntimeException("No GPU was selected");
        }
    }
    private VkPhysicalDeviceProperties getPhysicalDeviceProperties(VkPhysicalDevice physicalDevice){
        VkPhysicalDeviceProperties physicalDeviceProperties = VkPhysicalDeviceProperties.calloc();
        vkGetPhysicalDeviceProperties(physicalDevice, physicalDeviceProperties);
        return physicalDeviceProperties;
    }
    private VkDevice createDevice(VkPhysicalDevice physicalDevice) {

        VkDevice device;


        try(MemoryStack stack = stackPush()) {



            int[] uniqueQueueFamilies = IntStream.of(queueFamilies.graphicsFamily, queueFamilies.presentFamily).distinct().toArray();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for(int i = 0;i < uniqueQueueFamilies.length;i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            deviceFeatures.samplerAnisotropy(true);


            //Dynamic Rendering
            VkPhysicalDeviceDynamicRenderingFeaturesKHR physicalDeviceDynamicRenderingFeaturesKHR = VkPhysicalDeviceDynamicRenderingFeaturesKHR.calloc(stack);
            {
                physicalDeviceDynamicRenderingFeaturesKHR.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES_KHR);
                physicalDeviceDynamicRenderingFeaturesKHR.dynamicRendering(true);
            }

            //Compute Shader Derivatives
            VkPhysicalDeviceComputeShaderDerivativesFeaturesKHR physicalDeviceComputeShaderDerivativesFeaturesKHR = VkPhysicalDeviceComputeShaderDerivativesFeaturesKHR.calloc(stack);
            {
                physicalDeviceComputeShaderDerivativesFeaturesKHR.sType(KHRComputeShaderDerivatives.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_COMPUTE_SHADER_DERIVATIVES_FEATURES_KHR);
                physicalDeviceComputeShaderDerivativesFeaturesKHR.computeDerivativeGroupQuads(true);
            }

            //Unused Attachments
            VkPhysicalDeviceDynamicRenderingUnusedAttachmentsFeaturesEXT physicalDeviceDynamicRenderingUnusedAttachmentsFeaturesEXT = VkPhysicalDeviceDynamicRenderingUnusedAttachmentsFeaturesEXT.calloc(stack);
            {
                physicalDeviceDynamicRenderingUnusedAttachmentsFeaturesEXT.sType(EXTDynamicRenderingUnusedAttachments.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_UNUSED_ATTACHMENTS_FEATURES_EXT);
                physicalDeviceDynamicRenderingUnusedAttachmentsFeaturesEXT.dynamicRenderingUnusedAttachments(true);
            }

            VkPhysicalDeviceDescriptorIndexingFeatures descriptorIndexingFeatures = VkPhysicalDeviceDescriptorIndexingFeatures.calloc(stack);
            {
                descriptorIndexingFeatures.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES);
                descriptorIndexingFeatures.descriptorBindingPartiallyBound(true);

            }
            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo
                    .pNext(physicalDeviceDynamicRenderingFeaturesKHR)
                    .pNext(physicalDeviceComputeShaderDerivativesFeaturesKHR)
                    .pNext(physicalDeviceDynamicRenderingUnusedAttachmentsFeaturesEXT)
                    .pNext(descriptorIndexingFeatures);


            createInfo.pEnabledFeatures(deviceFeatures);

            PointerBuffer ppEnabledExtensionsNames = stack.mallocPointer(DEVICE_EXTENSIONS.size());

            DEVICE_EXTENSIONS.stream()
                    .map(stack::UTF8)
                    .forEach(ppEnabledExtensionsNames::put);

            ppEnabledExtensionsNames.rewind();

            createInfo.ppEnabledExtensionNames(ppEnabledExtensionsNames);


            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);

            if(vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to create logical device");
            }

            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

        }

        return device;
    }
    private VkQueue getGraphicsQueue(VkDevice device) {

        VkQueue graphicsQueue;
        try(MemoryStack stack = stackPush()) {
            PointerBuffer pGraphicsQueue = stack.pointers(VK_NULL_HANDLE);
            vkGetDeviceQueue(device, queueFamilies.graphicsFamily, 0, pGraphicsQueue);
            graphicsQueue = new VkQueue(pGraphicsQueue.get(0), device);
        }

        return graphicsQueue;
    }
    private VkQueue getComputeQueue(VkDevice device) {

        VkQueue computeQueue;
        try(MemoryStack stack = stackPush()) {
            PointerBuffer pComputeQueue = stack.pointers(VK_NULL_HANDLE);
            vkGetDeviceQueue(device, queueFamilies.computeFamily, 0, pComputeQueue);
            computeQueue = new VkQueue(pComputeQueue.get(0), device);
        }

        return computeQueue;
    }
    private VkQueue getPresentQueue(VkDevice device) {

        VkQueue presentQueue;
        try(MemoryStack stack = stackPush()) {
            PointerBuffer pPresentQueue = stack.pointers(VK_NULL_HANDLE);
            vkGetDeviceQueue(device, queueFamilies.presentFamily, 0, pPresentQueue);
            presentQueue = new VkQueue(pPresentQueue.get(0), device);
        }

        return presentQueue;
    }
    public VkDevice getDevice() {
        return device;
    }

    private VkSurfaceFormatKHR selectSwapchainSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {

        for(VkSurfaceFormatKHR availableFormat : availableFormats){
            if(availableFormat.format() == VK_FORMAT_R8G8B8A8_SRGB){
                return availableFormat;
            }
        }


        Logger.info(VulkanRenderer.class, "Unable to find sRGB VkSurfaceFormatKHR, using default. Colors may look incorrect.");

        return availableFormats.get(0);

    }
    private int selectSwapchainPresentMode(IntBuffer availablePresentModes, boolean vsync) {

        Function<Integer, Boolean> isPresentModeAvailable = presentMode -> {

            for(int i = 0; i < availablePresentModes.capacity(); i++){
                if(availablePresentModes.get(i) == presentMode) return true;
            }

            return false;
        };

        if(vsync) {
            if(isPresentModeAvailable.apply(VK_PRESENT_MODE_FIFO_KHR)) return VK_PRESENT_MODE_FIFO_KHR;
            else throw new SkyRuntimeException("Vsync was requested but VK_PRESENT_MODE_FIFO_KHR is not available as a present mode");
        }
        else {
            if(isPresentModeAvailable.apply(VK_PRESENT_MODE_IMMEDIATE_KHR)) return VK_PRESENT_MODE_IMMEDIATE_KHR;
            else throw new SkyRuntimeException("Vsync was requested to be disabled but VK_PRESENT_MODE_IMMEDIATE_KHR is not available as a present mode");
        }

    }
    private VkExtent2D selectSwapchainExtent(VkSurfaceCapabilitiesKHR capabilities, int width, int height) {
        VkExtent2D extent = VkExtent2D.calloc().set(width, height);

        if(capabilities.currentExtent().width() != UINT64_MAX) {
            return extent;
        }

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        extent.width(clamp(extent.width(), minExtent.width(), maxExtent.width()));
        extent.height(clamp(extent.height(), minExtent.height(), maxExtent.height()));



        return extent;
    }

    @Override
    public void syncWithSurface(boolean surfaceInvalidated) {
        if (surfaceInvalidated) {
            swapchain.disposeAll();
            this.removeDisposable(swapchain);
            swapchainRenderTarget.disposeAll();
            this.removeDisposable(swapchainRenderTarget);

            this.width = surface.getWidth();
            this.height = surface.getHeight();

            swapchainRenderTarget = createSwapchainRenderTarget(settings);
            frameIndex = 0;
        }

        vkWaitForFences(device, submissionFences[frameIndex].getHandle(), true, UINT64_MAX);
        vkResetFences(device, submissionFences[frameIndex].getHandle());
    }

    @Override
    public void submit(List<Pass> passes) {


        try(MemoryStack stack = stackPush()) {
            IntBuffer pImageIndex = stack.callocInt(1);
            vkAcquireNextImageKHR(
                    device,
                    swapchain.getHandle(),
                    UINT64_MAX,
                    ((VulkanSemaphore[]) swapchainImageAcquireSemaphores)[frameIndex].getHandle(),
                    VK_NULL_HANDLE,
                    pImageIndex
            );

            int waitDstStageMask = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            VulkanSemaphore wait = ((VulkanSemaphore[]) swapchainImageAcquireSemaphores)[frameIndex];

            for (Pass pass : passes) {
                //Insert barriers
                {
                    pass.setStartingBarriers(object -> {

                        VkCommandBuffer commandBuffer = (VkCommandBuffer) object;

                        for (Dependency rd : pass.getDependencies()) {
                            RenderGraphResource rgResource = rd.getResource();

                            assert rgResource != null : "All resource dependencies must be fully realized by the time the graph is submitted to the renderer";

                            Texture[] textures = null;
                            Buffer[] buffers = null;

                            switch (rgResource.get()) {
                                case Buffer[] buffer: {
                                    buffers = buffer;
                                    break;
                                }
                                case Texture[] texture: {
                                    textures = texture;
                                    break;
                                }
                                default :
                            }


                            if (textures != null) {
                                int texturePairs = textures.length / maxFramesInFlight;
                                for (int i = 0; i < texturePairs; i++) {
                                    Texture texture = textures[i * maxFramesInFlight + frameIndex];
                                    VulkanImage image = ((VulkanTexture) texture).getImage();
                                    VulkanImageView imageView = ((VulkanTexture) texture).getImageView();
                                    switch (rd.getAccessType()) {
                                        case AccessTypes.DepthReadWrite: {
                                            VkImageMemoryBarrier.Buffer imageBarrier = VkImageMemoryBarrier.calloc(1, stack);
                                            {
                                                imageBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                                                imageBarrier.oldLayout(image.getLastLayout());
                                                imageBarrier.newLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
                                                imageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                                imageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                                imageBarrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT);
                                                imageBarrier.image(image.getHandle());
                                                imageBarrier.subresourceRange().aspectMask(imageView.getAspectMask());
                                                imageBarrier.subresourceRange().baseMipLevel(0);
                                                imageBarrier.subresourceRange().levelCount(1);
                                                imageBarrier.subresourceRange().baseArrayLayer(0);
                                                imageBarrier.subresourceRange().layerCount(1);
                                            }
                                            vkCmdPipelineBarrier(
                                                    commandBuffer,
                                                    VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT,
                                                    VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT,
                                                    0,
                                                    null,
                                                    null,
                                                    imageBarrier
                                            );
                                            image.setCurrentLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
                                            break;
                                        }
                                        case AccessTypes.ShaderRead: {
                                            VkImageMemoryBarrier.Buffer imageBarrier = VkImageMemoryBarrier.calloc(1, stack);
                                            {
                                                imageBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                                                imageBarrier.oldLayout(image.getLastLayout());
                                                imageBarrier.newLayout(VK_IMAGE_LAYOUT_GENERAL);
                                                imageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                                imageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                                imageBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                                                imageBarrier.image(image.getHandle());
                                                imageBarrier.subresourceRange().aspectMask(imageView.getAspectMask());
                                                imageBarrier.subresourceRange().baseMipLevel(0);
                                                imageBarrier.subresourceRange().levelCount(1);
                                                imageBarrier.subresourceRange().baseArrayLayer(0);
                                                imageBarrier.subresourceRange().layerCount(1);
                                            }
                                            vkCmdPipelineBarrier(
                                                    commandBuffer,
                                                    imageView.getAspectMask() == VK_IMAGE_ASPECT_COLOR_BIT ? VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT : VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT,
                                                    pass instanceof VulkanGraphicsPass ? VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT : VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                                                    0,
                                                    null,
                                                    null,
                                                    imageBarrier
                                            );
                                            image.setCurrentLayout(VK_IMAGE_LAYOUT_GENERAL);
                                            break;
                                        }
                                        case AccessTypes.DepthWrite: {
                                            VkImageMemoryBarrier.Buffer imageBarrier = VkImageMemoryBarrier.calloc(1, stack);
                                            {
                                                imageBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                                                imageBarrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                                                imageBarrier.newLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
                                                imageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                                imageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                                imageBarrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
                                                imageBarrier.image(image.getHandle());
                                                imageBarrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
                                                imageBarrier.subresourceRange().baseMipLevel(0);
                                                imageBarrier.subresourceRange().levelCount(1);
                                                imageBarrier.subresourceRange().baseArrayLayer(0);
                                                imageBarrier.subresourceRange().layerCount(1);
                                            }
                                            vkCmdPipelineBarrier(
                                                    commandBuffer,
                                                    VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                                                    VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT,
                                                    0,
                                                    null,
                                                    null,
                                                    imageBarrier
                                            );
                                            image.setCurrentLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
                                            break;
                                        }
                                        case AccessTypes.ColorWrite: {
                                            VkImageMemoryBarrier.Buffer imageBarrier = VkImageMemoryBarrier.calloc(1, stack);
                                            {
                                                imageBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                                                imageBarrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                                                imageBarrier.newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                                                imageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                                imageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                                imageBarrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                                                imageBarrier.image(image.getHandle());
                                                imageBarrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                                                imageBarrier.subresourceRange().baseMipLevel(0);
                                                imageBarrier.subresourceRange().levelCount(1);
                                                imageBarrier.subresourceRange().baseArrayLayer(0);
                                                imageBarrier.subresourceRange().layerCount(1);
                                            }
                                            vkCmdPipelineBarrier(
                                                    commandBuffer,
                                                    VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                                                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                                                    0,
                                                    null,
                                                    null,
                                                    imageBarrier
                                            );
                                            image.setCurrentLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                                            break;
                                        }
                                    }

                                }
                            }
                            if (buffers != null) {
                                int bufferPairs = buffers.length / maxFramesInFlight;
                                for (int i = 0; i < bufferPairs; i++) {
                                    VulkanBuffer buffer = (VulkanBuffer) buffers[i * maxFramesInFlight + frameIndex];

                                    switch (rd.getAccessType()) {
                                        case AccessTypes.ShaderWrite -> {
                                            VkBufferMemoryBarrier.Buffer bufferBarrier = VkBufferMemoryBarrier.calloc(1, stack);
                                            bufferBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER);
                                            bufferBarrier.srcAccessMask(VK_ACCESS_SHADER_READ_BIT);
                                            bufferBarrier.dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT);
                                            bufferBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                            bufferBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                            bufferBarrier.buffer(buffer.getHandle());
                                            bufferBarrier.size(VK_WHOLE_SIZE);

                                            vkCmdPipelineBarrier(
                                                    commandBuffer,
                                                    VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                                                    VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                                                    0,
                                                    null,
                                                    bufferBarrier,
                                                    null
                                            );
                                            break;
                                        }
                                        case AccessTypes.ShaderRead -> {
                                            VkBufferMemoryBarrier.Buffer bufferBarrier = VkBufferMemoryBarrier.calloc(1, stack);
                                            bufferBarrier.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER);
                                            bufferBarrier.srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT);
                                            bufferBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                                            bufferBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                            bufferBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                            bufferBarrier.buffer(buffer.getHandle());
                                            bufferBarrier.size(VK_WHOLE_SIZE);

                                            vkCmdPipelineBarrier(
                                                    commandBuffer,
                                                    VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                                                    VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                                                    0,
                                                    null,
                                                    bufferBarrier,
                                                    null
                                            );
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    });
                    pass.setEndingBarriers(object -> {
                        VkCommandBuffer commandBuffer = (VkCommandBuffer) object;

                        for (Dependency rd : pass.getDependencies()) {
                            RenderGraphResource rgResource = rd.getResource();

                            assert rgResource != null : "All resource dependencies must be fully realized by the time the graph is submitted to the renderer";

                            Texture[] textures = null;
                            if (rgResource.get() instanceof Texture[]) {
                                textures = ((Texture[]) rgResource.get());
                            }

                            if (textures != null) {
                                int texturePairs = textures.length / maxFramesInFlight;
                                for (int i = 0; i < texturePairs; i++) {
                                    Texture texture = textures[i * maxFramesInFlight + frameIndex];
                                    VulkanImage image = ((VulkanTexture) texture).getImage();
                                    switch (rd.getAccessType()) {


                                        case AccessTypes.Present: {
                                            VkImageMemoryBarrier.Buffer imageBarrier = VkImageMemoryBarrier.calloc(1, stack);
                                            {
                                                imageBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                                                imageBarrier.oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                                                imageBarrier.newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
                                                imageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                                imageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                                                imageBarrier.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                                                imageBarrier.image(image.getHandle());
                                                imageBarrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                                                imageBarrier.subresourceRange().baseMipLevel(0);
                                                imageBarrier.subresourceRange().levelCount(1);
                                                imageBarrier.subresourceRange().baseArrayLayer(0);
                                                imageBarrier.subresourceRange().layerCount(1);
                                            }
                                            vkCmdPipelineBarrier(
                                                    commandBuffer,
                                                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                                                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                                                    0,
                                                    null,
                                                    null,
                                                    imageBarrier
                                            );
                                            break;
                                        }


                                    }

                                }
                            }
                        }
                    });
                }


                pass.startRecording(frameIndex);
                pass.resolveStartingBarriers();
                pass.getRecorder().run();
                pass.resolveEndingBarriers();
                pass.endRecording();

                {
                    VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
                    submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
                    submitInfo.waitSemaphoreCount(1);

                    submitInfo.pWaitSemaphores(stack.longs(wait.getHandle()));
                    submitInfo.pWaitDstStageMask(stack.ints(waitDstStageMask));
                    submitInfo.pCommandBuffers(stack.pointers(pass instanceof VulkanGraphicsPass ? ((VulkanGraphicsPass) pass).getCommandBuffers()[frameIndex] : ((VulkanComputePass) pass).getCommandBuffers()[frameIndex]));
                    submitInfo.pSignalSemaphores(stack.longs(((VulkanSemaphore) pass.getFinishedSemaphores()[frameIndex]).getHandle()));

                    vkQueueSubmit(graphicsQueue, submitInfo, pass == passes.getLast() ? ((VulkanFence[]) submissionFences)[frameIndex].getHandle() : VK_NULL_HANDLE);

                    waitDstStageMask = pass instanceof VulkanGraphicsPass ? VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT : VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;
                    wait = (VulkanSemaphore) pass.getFinishedSemaphores()[frameIndex];
                }
            }








            VulkanSemaphore[] finishedSemaphores = (VulkanSemaphore[]) passes.getLast().getFinishedSemaphores();

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(stack.longs(finishedSemaphores[frameIndex].getHandle()));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapchain.getHandle()));
            presentInfo.pImageIndices(pImageIndex);

            vkQueuePresentKHR(presentQueue, presentInfo);
            frameIndex = (frameIndex + 1) % FRAMES_IN_FLIGHT;
        }
    }

    @Override
    public String getDeviceName() {
        return physicalDeviceProperties.deviceNameString();
    }

    @Override
    public void waitForDevice() {
        vkDeviceWaitIdle(device);
    }



    @Override
    public void dispose() {
        vkDeviceWaitIdle(device);


        vmaDestroyAllocator(allocator.getId());
        vkDestroySurfaceKHR(instance, vkSurface, null);
        if(settings.validation)
            EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);

        physicalDeviceProperties.free();
        vkDestroyDevice(device, null);

        vkDestroyInstance(instance, null);
    }
}