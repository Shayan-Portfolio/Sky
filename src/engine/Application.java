package engine;

import engine.graphics.Disposable;

public abstract class Application extends Disposable {
    protected Surface surface;
    protected float startTime;

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
