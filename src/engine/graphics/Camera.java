package engine.graphics;

import engine.util.MathUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private Matrix4f proj, view;
    private boolean invertY;
    private Matrix4f invProj = new Matrix4f(), invView = new Matrix4f();
    private float zNear, zFar, fovY;


    public static Camera newPerspectiveCamera(Vector3f pos,
                                           Vector3f lookAt,
                                           Vector3f up,
                                           float fovY,
                                           float zNear,
                                           float zFar,
                                           float aspectRatio,
                                           boolean zeroToOne,
                                           boolean invertY) {

        Camera camera = new Camera();
        camera.setView(new Matrix4f().lookAt(pos, lookAt, up));
        camera.setProj(new Matrix4f().perspective(fovY, aspectRatio, zNear, zFar, zeroToOne));
        camera.invertY = invertY;
        if(camera.invertY) camera.proj.m11(camera.proj.m11() * -1);
        camera.zNear = zNear;
        camera.zFar = zFar;
        camera.fovY = fovY;

        return camera;
    }



    public static Camera newOrthoCamera(int width,
                                            int height,
                                            float zNear,
                                            float zFar,
                                            boolean zZeroToOne,
                                            boolean invertY) {

        Camera camera = new Camera();
        camera.setView(new Matrix4f().identity());
        camera.setProj(
                new Matrix4f().ortho(0,
                        width,
                        0,
                        height,
                        zNear,
                        zFar,
                        zZeroToOne
                )
        );
        camera.invertY = invertY;
        if(camera.invertY) camera.proj.m11(camera.proj.m11() * -1);
        camera.zNear = zNear;
        camera.zFar = zFar;
        camera.fovY = -1;

        return camera;
    }


    public boolean isInvertY() {
        return invertY;
    }

    public float getzNear() {
        return zNear;
    }

    public float getzFar() {
        return zFar;
    }

    public float getFovY() {
        return fovY;
    }

    public Matrix4f getProj() {
        return proj;
    }

    public void setProj(Matrix4f proj) {
        this.proj = proj;
        proj.invert(this.invProj);
    }

    public Matrix4f getInvProj() {
        return invProj;
    }

    public Matrix4f getInvView() {
        return invView;
    }

    public Matrix4f getView() {
        return view;
    }

    public void setView(Matrix4f view) {
        this.view = view;
        view.invert(this.invView);
    }
}
