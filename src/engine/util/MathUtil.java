package engine.util;

import org.joml.*;

import javax.vecmath.Quat4f;

public class MathUtil {
    public static final int MATRIX_SIZE_BYTES = 4 * 4 * Float.BYTES;
    public static final int VEC3_SIZE_BYTES = 3 * Float.BYTES;
    private MathUtil() {}

    public static Vector3f[] getFrustum(Matrix4f invView, Matrix4f invProj, float depth) {
        Vector3f tr = MathUtil.unproject(new Vector2f(1f, -1f), depth, invView, invProj);
        Vector3f tl = MathUtil.unproject(new Vector2f(-1f, -1f), depth, invView, invProj);
        Vector3f br = MathUtil.unproject(new Vector2f(1f, 1f), depth, invView, invProj);
        Vector3f bl = MathUtil.unproject(new Vector2f(-1f, 1f), depth, invView, invProj);

        return new Vector3f[]{tr, tl, br, bl};
    }




    public static boolean isSphereInFrustum(Vector3f P, float radius, Vector3f frustumCenter, Vector3f[] F) {
        //Ax + By + Cz - d = 0
        Vector3f sphereCenter = P.sub(frustumCenter, new Vector3f());

        Vector3f[] frustum = new Vector3f[F.length];

        for (int i = 0; i < frustum.length; i++)
            F[i].sub(frustumCenter, frustum[i] = new Vector3f());

        int success = 0;

        //Far
        {

            Vector3f A = frustum[2].sub(frustum[0], new Vector3f());
            Vector3f B = frustum[3].sub(frustum[0], new Vector3f());
            Vector3f N = A.cross(B).normalize();
            float d = frustum[0].dot(N);
            float dist = (N.dot(sphereCenter) - d);
            if(dist > 0 || -dist < radius) success++;
        }


        //Left
        {

            Vector3f A = frustum[1];
            Vector3f B = frustum[3];
            Vector3f N = A.cross(B, new Vector3f()).normalize();
            float d = frustum[1].dot(N);
            float dist = (N.dot(sphereCenter) - d);
            if(dist > 0 || -dist < radius) success++;
        }

        //Right
        {

            Vector3f A = frustum[0];
            Vector3f B = frustum[2];
            Vector3f N = B.cross(A, new Vector3f()).normalize();
            float d = frustum[0].dot(N);
            float dist = (N.dot(sphereCenter) - d);
            if(dist > 0 || -dist < radius) success++;
        }

        //Bottom
        {

            Vector3f A = frustum[2];
            Vector3f B = frustum[3];
            Vector3f N = B.cross(A, new Vector3f()).normalize();
            float d = frustum[2].dot(N);
            float dist = (N.dot(sphereCenter) - d);
            if(dist > 0 || -dist < radius) success++;
        }

        //Top
        {

            Vector3f A = frustum[0];
            Vector3f B = frustum[1];
            Vector3f N = A.cross(B, new Vector3f()).normalize();
            float d = frustum[0].dot(N);
            float dist = (N.dot(sphereCenter) - d);
            if(dist > 0 || -dist < radius) success++;
        }




        return success == 5;
    }


    public static Vector3f unproject(Vector2f ndcXY, float depth, Matrix4f invView, Matrix4f invProj) {
        float ndcX = ndcXY.x;
        float ndcY = ndcXY.y;
        Vector4f start = new Vector4f(ndcX, ndcY, depth, 1.0f);
        Vector4f view = invProj.transform(start);
        Vector4f w = invView.transform(view);

        w.div(w.w);

        return w.xyz(new Vector3f());
    }

    public static Vector4f project(Vector4f position, int w, int h, Matrix4f view, Matrix4f proj) {
        Vector4f p = new Vector4f(position);
        view.transform(p);
        proj.transform(p);
        p.div(p.w);


        p.mul(0.5f);
        p.add(0.5f, 0.5f, 0, 0);

        p.mul(w, h, 0, 0);

        return p;
    }


}
