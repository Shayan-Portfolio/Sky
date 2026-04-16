package engine;

import engine.graphics.Disposable;
import engine.graphics.RenderAPI;
import engine.graphics.RendererSettings;
import engine.input.SurfaceCharCallback;
import engine.input.SurfaceKeyCallback;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

import java.util.ArrayList;
import java.util.List;

public abstract class Surface extends Disposable {
    protected String title;
    protected int width, height;
    protected boolean resizable;
    protected Vector2f cursorPos = new Vector2f();


    //Vulkan specific stuff
    protected long vkDebugMessenger;
    protected long vkSurface;
    protected VkInstance vkInstance;
    protected VkDebugUtilsMessengerCallbackEXT vkDebugUtilsMessengerCallbackEXT;

    public abstract void requestRenderAPI(RenderAPI api, RendererSettings settings);

    protected List<SurfaceKeyCallback> surfaceKeyCallbacks = new ArrayList<>();
    protected List<SurfaceCharCallback> surfaceCharCallbacks = new ArrayList<>();


    public enum Cursor {
        Default,
        ResizeWE
    }

    public Surface(Disposable parent, String title, int width, int height, boolean resizable) {
        super(parent);
        this.title = title;
        this.width = width;
        this.height = height;
        this.resizable = resizable;
    }

    public String getTitle() {
        return title;
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }

    public void addKeyCallback(SurfaceKeyCallback callback) {
        surfaceKeyCallbacks.add(callback);
    }

    public void addCharCallback(SurfaceCharCallback callback) {
        surfaceCharCallbacks.add(callback);
    }

    public abstract String getClipboardString();

    public abstract boolean getKeyPressed(int key);
    public abstract boolean getKeyReleased(int key);
    public abstract boolean getMousePressed(int button);
    public abstract boolean getMouseReleased(int button);
    public abstract Vector2f getMousePos();
    public abstract void setCursor(Cursor cursor);
    public abstract boolean isMouseCaptured();


    public static Surface newSurface(Disposable parent, String title, int width, int height) {
        return newSurface(parent, title, width, height, true);
    }
    public static Surface newSwingSurface(Disposable parent, String title, int width, int height) {
        return new EditorSurface(parent, title, width, height, true);
    }
    public static Surface newSurface(Disposable parent, String title, int width, int height, boolean resizable) {
        return new GLFWSurface(parent, title, width, height, resizable);
    }




    public boolean isResizable() {
        return resizable;
    }
    public abstract PointerBuffer getVulkanInstanceExtensions();
    public long getVulkanSurface() {
        return vkSurface;
    }
    public VkInstance getVulkanInstance() {
        return vkInstance;
    }
    public abstract void setCaptureMouse(boolean captureMouse);

    protected abstract VkInstance createInstance(String appName, List<String> validationLayers);



    public long getVulkanDebugMessenger() {
        return vkDebugMessenger;
    }

    public abstract boolean supportsRenderAPI(RenderAPI api);
    public abstract double getTime();
    public abstract void display();
    public abstract boolean update();
    public abstract boolean shouldClose();




}



