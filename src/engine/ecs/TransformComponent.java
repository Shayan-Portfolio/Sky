package engine.ecs;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@ComponentArray(mask = 1 << 7)
public record TransformComponent(Matrix4f transform) {
    public void setPosition(float x, float y, float z) {
        transform.m30(x);
        transform.m31(y);
        transform.m32(z);
    }


    public Vector3f getPosition(Vector3f d) {
        d.x = transform.m30();
        d.y = transform.m31();
        d.z = transform.m32();
        return d;
    }

}
