package engine.ecs;

import engine.graphics.Camera;

@ComponentArray(mask = 1 << 2)
public class CameraComponent implements Component {
    public Camera camera;

    public CameraComponent(Camera camera) {
        this.camera = camera;
    }

    @Override
    public void removed(Actor a) {}
}
