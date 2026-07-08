package engine;

import engine.graphics.Disposable;
import engine.wsi.Surface;

public abstract class Application extends Disposable {
    protected Surface surface;
    protected float startTime;

    protected static Application instance;

    public static Application get() {
        return instance;
    }

    public static void set(Application instance) {
        Application.instance = instance;
    }

    public Application() {
        super(null);
    }

    public void launch(String[] args, Surface surface){
        init(args, surface);
        startTime = (float) surface.getTime();
    }

    public void close(){
        //disposeAll();
    }

    public Surface getSurface() {
        return surface;
    }

    public float getStartTime() {
        return startTime;
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
    }

    public void init(String[] args, Surface surface) {
        this.surface = surface;
    }
    public abstract boolean update();
    public abstract void dispose();
}
