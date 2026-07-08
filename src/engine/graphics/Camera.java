package engine.graphics;

import engine.util.MathUtil;
import org.joml.Matrix4f;

public class Camera {
    private Matrix4f proj, view;
    private boolean invertY;
    public static final int SIZE = MathUtil.MATRIX_SIZE_BYTES * 2;
    private Matrix4f invProj = new Matrix4f(), invView = new Matrix4f();

    public Camera(Matrix4f view, Matrix4f proj, boolean invertY) {
        setView(view);
        setProj(proj);

        this.invertY = invertY;

        if(invertY){
            this.proj.m11(this.proj.m11() * -1);
        }
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
