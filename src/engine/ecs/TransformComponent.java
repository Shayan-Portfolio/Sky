package engine.ecs;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@ComponentArray(mask = 1 << 7)
public record TransformComponent(Matrix4f... transforms) {
    public void setPosition(int index, float x, float y, float z) {
        transforms[index].m30(x);
        transforms[index].m31(y);
        transforms[index].m32(z);
    }


    public Vector3f getPosition(int index, Vector3f d) {
        d.x = transforms[index].m30();
        d.y = transforms[index].m31();
        d.z = transforms[index].m32();
        return d;
    }

    public Vector4f getPosition(int index, Vector4f d) {
        d.x = transforms[index].m30();
        d.y = transforms[index].m31();
        d.z = transforms[index].m32();
        d.w = 1;
        return d;
    }

}
