package engine.ecs;

import org.joml.Matrix4f;

@ComponentArray(mask = 1 << 7)
public record TransformComponent(Matrix4f transform) {
    public void setPosition(float x, float y, float z) {
        transform.m30(x);
        transform.m31(y);
        transform.m32(z);
    }
}
