package engine.graphics.vulkan;

import org.lwjgl.vulkan.*;

public class VulkanRuntime {

    private static VkDevice currentDevice;
    private static VkPhysicalDevice currentPhysicalDevice;
    private static int graphicsFamilyIndex;
    private static VkQueue graphicsQueue;
    private static VkQueue computeQueue;
    private static VkPhysicalDeviceProperties physicalDeviceProperties;
    private static int computeFamilyIndex;
    private static boolean validation = false;

    public static VkDevice getCurrentDevice() {
        return currentDevice;
    }

    public static void setCurrentDevice(VkDevice currentDevice) {
        VulkanRuntime.currentDevice = currentDevice;
    }

    public static VkPhysicalDevice getCurrentPhysicalDevice() {
        return currentPhysicalDevice;
    }

    public static void setCurrentPhysicalDevice(VkPhysicalDevice currentPhysicalDevice) {
        VulkanRuntime.currentPhysicalDevice = currentPhysicalDevice;
    }

    public static boolean hasValidation() {
        return validation;
    }

    public static void setValidation(boolean validation) {
        VulkanRuntime.validation = validation;
    }

    public static int getGraphicsFamilyIndex() {
        return graphicsFamilyIndex;
    }

    public static int getComputeFamilyIndex() {
        return computeFamilyIndex;
    }

    public static void setGraphicsFamilyIndex(int graphicsFamilyIndex) {
        VulkanRuntime.graphicsFamilyIndex = graphicsFamilyIndex;
    }

    public static void setComputeFamilyIndex(int computeFamilyIndex) {
        VulkanRuntime.computeFamilyIndex = computeFamilyIndex;
    }

    public static VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public static void setGraphicsQueue(VkQueue graphicsQueue) {
        VulkanRuntime.graphicsQueue = graphicsQueue;
    }

    public static VkQueue getComputeQueue() {
        return computeQueue;
    }
    public static void setComputeQueue(VkQueue computeQueue) {
        VulkanRuntime.computeQueue = computeQueue;
    }

    public static VkPhysicalDeviceProperties getPhysicalDeviceProperties() {
        return physicalDeviceProperties;
    }

    public static void setPhysicalDeviceProperties(VkPhysicalDeviceProperties physicalDeviceProperties) {
        VulkanRuntime.physicalDeviceProperties = physicalDeviceProperties;
    }
}
