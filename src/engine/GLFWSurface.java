package engine;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

import engine.graphics.Disposable;
import engine.graphics.RenderAPI;
import engine.graphics.RendererSettings;
import engine.graphics.vulkan.VulkanRenderer;
import engine.input.SurfaceCharCallback;
import engine.input.SurfaceKeyCallback;
import engine.logging.Logger;
import engine.logging.SkyRuntimeException;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Set;


public class GLFWSurface extends Surface {

    private long handle;
    private Cursor cursor;
    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallbackI keyCallbackI;
    private GLFWCharCallbackI charCallbackI;



    @Override
    public void requestRenderAPI(RenderAPI api, RendererSettings settings) {
        if(api == RenderAPI.Vulkan) {
            try (MemoryStack stack = stackPush()) {
                vkInstance = createInstance(title, settings.validation ? List.of("VK_LAYER_KHRONOS_validation") : null);
                LongBuffer pSurface = stack.mallocLong(1);
                glfwCreateWindowSurface(vkInstance, handle,
                        null, pSurface);
                vkSurface = pSurface.get(0);
            }
        }
    }

    public GLFWSurface(Disposable parent, String title, int width, int height, boolean resizable) {
        super(parent, title, width, height, resizable);



        if (!glfwInit()) {
            throw new SkyRuntimeException("Failed to initialize GLFW");
        }

        errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();

        glfwWindowHint(GLFW_RESIZABLE, glfwBool(resizable));
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        handle = glfwCreateWindow(width, height, title, NULL, NULL);

        keyCallbackI = (window, key, scancode, action, mods) -> {
            for(SurfaceKeyCallback callback : surfaceKeyCallbacks) {
                if(action == GLFW_PRESS || action == GLFW_REPEAT) callback.keyClick(key, mods);
            }
        };

        charCallbackI = (window, codepoint) -> {
            for(SurfaceCharCallback callback : surfaceCharCallbacks) {
                callback.keyClick(Character.toChars(codepoint)[0]);
            }
        };


        glfwSetKeyCallback(handle, keyCallbackI);
        glfwSetCharCallback(handle, charCallbackI);

    }

    @Override
    public PointerBuffer getVulkanInstanceExtensions() {
        return glfwGetRequiredInstanceExtensions();
    }

    @Override
    public void setCaptureMouse(boolean captureMouse) {
        glfwSetInputMode(handle, GLFW_CURSOR, captureMouse ? GLFW_CURSOR_DISABLED :  GLFW_CURSOR_NORMAL);
    }

    @Override
    protected VkInstance createInstance(String appName, List<String> validationLayers){

        boolean validation = validationLayers != null;

        VkInstance instance;



        try (MemoryStack stack = stackPush()) {


            if(validation) {


                IntBuffer layerCount = stack.ints(0);

                vkEnumerateInstanceLayerProperties(layerCount, null);

                VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(layerCount.get(0), stack);

                vkEnumerateInstanceLayerProperties(layerCount, availableLayers);

                Set<String> availableLayerNames = availableLayers.stream()
                        .map(VkLayerProperties::layerNameString)
                        .collect(toSet());

                for(String validationLayerName : validationLayers){
                    if(!availableLayerNames.contains(validationLayerName)){
                        throw new SkyRuntimeException("Validation Layer " + validationLayerName + " is not available");
                    }
                    else {
                        Logger.info(VulkanRenderer.class, validationLayerName);
                    }
                }
            }

            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);

            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe(appName));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8Safe(appName));
            appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK_API_VERSION_1_3);

            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(stack);

            instanceCreateInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            instanceCreateInfo.pApplicationInfo(appInfo);




            PointerBuffer pInstanceExtensions;
            PointerBuffer pWindowingExtensions = getVulkanInstanceExtensions();


            int instanceExtensionCount = pWindowingExtensions.capacity();
            if(validation) instanceExtensionCount++;


            pInstanceExtensions = stack.mallocPointer(instanceExtensionCount);
            pInstanceExtensions.put(pWindowingExtensions);


            if(validation)
                pInstanceExtensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));


            instanceCreateInfo.ppEnabledExtensionNames(pInstanceExtensions.rewind());




            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = null;




            if (validation) {

                PointerBuffer pEnabledLayerNames = stack.mallocPointer(validationLayers.size());

                for(String validationLayerName : validationLayers){
                    pEnabledLayerNames.put(stack.UTF8(validationLayerName));
                }

                instanceCreateInfo.ppEnabledLayerNames(pEnabledLayerNames.rewind());

                vkDebugUtilsMessengerCallbackEXT = new VkDebugUtilsMessengerCallbackEXT() {
                    @Override
                    public int invoke(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
                        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                        Logger.info(VulkanRenderer.class, callbackData.pMessageString());

                        return VK_FALSE;
                    }
                };



                debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
                debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
                debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
                debugCreateInfo.pfnUserCallback(vkDebugUtilsMessengerCallbackEXT);

                instanceCreateInfo.pNext(debugCreateInfo.address());
            }





            PointerBuffer instancePtr = stack.mallocPointer(1);

            if (vkCreateInstance(instanceCreateInfo, null, instancePtr) != VK_SUCCESS) {
                throw new SkyRuntimeException("Failed to create instance");
            }

            instance = new VkInstance(instancePtr.get(0), instanceCreateInfo);



            if(validation) {

                LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);

                int result = 0;

                if (vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
                    result = vkCreateDebugUtilsMessengerEXT(instance, debugCreateInfo, null, pDebugMessenger) == VK_SUCCESS ? VK_SUCCESS : VK_ERROR_EXTENSION_NOT_PRESENT;
                }

                if (result != VK_SUCCESS)
                    throw new SkyRuntimeException("Failed to create the debug messenger as the extension is not present");

                vkDebugMessenger = pDebugMessenger.get(0);


            }
        }

        return instance;
    }


    @Override
    public boolean supportsRenderAPI(RenderAPI api) {
        if(api == RenderAPI.Vulkan) return glfwVulkanSupported();
        return false;
    }

    @Override
    public double getTime() {
        return glfwGetTime();
    }


    private int glfwBool(boolean b){
        return b ? GLFW_TRUE : GLFW_FALSE;
    }

    @Override
    public void display() {
        if (handle == NULL)
            throw new SkyRuntimeException("Failed to create GLFW window");
        glfwShowWindow(handle);

    }

    @Override
    public boolean update() {

        double[] x = new double[1], y = new double[1];
        glfwGetCursorPos(handle, x, y);

        cursorPos.set(x[0], y[0]);
        glfwPollEvents();

        int newWidth = getWidth();
        int newHeight = getHeight();

        while(newWidth == 0 || newHeight == 0) {
            newWidth = getWidth();
            newHeight = getHeight();

            glfwPollEvents();
        }


        if(newWidth != width || newHeight != height) {
            width = newWidth;
            height = newHeight;

            return true;
        }


        return false;
    }

    @Override
    public int getWidth() {
        int[] width = new int[1], height = new int[1];
        glfwGetWindowSize(handle, width, height);
        return width[0];
    }

    @Override
    public int getHeight() {
        int[] width = new int[1], height = new int[1];
        glfwGetWindowSize(handle, width, height);
        return height[0];
    }

    @Override
    public String getClipboardString() {
        return glfwGetClipboardString(handle);
    }


    @Override
    public boolean getKeyPressed(int key) {
        return glfwGetKey(handle, key) == GLFW_PRESS;
    }

    @Override
    public boolean getKeyReleased(int key) {
        return glfwGetKey(handle, key) == GLFW_RELEASE;
    }

    @Override
    public boolean getMousePressed(int button) {
        return glfwGetMouseButton(handle, button) == GLFW_PRESS;
    }

    @Override
    public boolean getMouseReleased(int button) {
        return glfwGetMouseButton(handle, button) == GLFW_RELEASE;
    }

    @Override
    public Vector2f getMousePos() {
        return cursorPos;
    }

    @Override
    public void setCursor(Cursor cursor) {
        if(this.cursor != cursor) {
            switch (cursor) {
                case Default -> glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_ARROW_CURSOR));
                case ResizeWE -> glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_RESIZE_EW_CURSOR));
            }
            this.cursor = cursor;
        }
    }

    @Override
    public boolean isMouseCaptured() {
        int mode = glfwGetInputMode(handle, GLFW_CURSOR);
        return mode == GLFW_CURSOR_DISABLED;
    }

    @Override
    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    @Override
    public void dispose() {

        if(vkDebugUtilsMessengerCallbackEXT != null) vkDebugUtilsMessengerCallbackEXT.free();
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
        errorCallback.free();
        glfwTerminate();
    }


}
