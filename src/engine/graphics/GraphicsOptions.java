package engine.graphics;

public class GraphicsOptions {
    public boolean validation = true;
    public GraphicsAPIs backend;
    public boolean vsync;

    public GraphicsOptions(GraphicsAPIs backend) {
        this.backend = backend;
    }

    public GraphicsOptions validation(boolean validation){
        this.validation = validation;
        return this;
    }

    public GraphicsOptions vsync(boolean vsync){
        this.vsync = vsync;
        return this;
    }


}
