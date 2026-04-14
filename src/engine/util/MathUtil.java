package engine.util;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.vecmath.Quat4f;

public class MathUtil {
    private MathUtil() {}

    public static void copy(Vector3f src, javax.vecmath.Vector3f dst) {
        dst.x = src.x;
        dst.y = src.y;
        dst.z = src.z;
    }

    public static void copy(javax.vecmath.Vector3f src, Vector3f dst) {
        dst.x = src.x;
        dst.y = src.y;
        dst.z = src.z;
    }

    public static void copy(Quaternionf src, javax.vecmath.Quat4f dst) {
        dst.x = src.x;
        dst.y = src.y;
        dst.z = src.z;
        dst.w = src.w;
    }

    public static void copy(javax.vecmath.Quat4f src, Quaternionf dst) {
        dst.x = src.x;
        dst.y = src.y;
        dst.z = src.z;
        dst.w = src.w;
    }

    public static javax.vecmath.Vector3f vec3(Vector3f src) {
        return new javax.vecmath.Vector3f(src.x, src.y, src.z);
    }

    public static Vector3f vec3(javax.vecmath.Vector3f src) {
        return new Vector3f(src.x, src.y, src.z);
    }

    public static Vector4f vec4(javax.vecmath.Vector3f src) {
        return new Vector4f(src.x, src.y, src.z, 1);
    }


    public static void scaleMax(javax.vecmath.Vector3f v, float scale) {
        int axis = 0;

        if(v.y > v.x) axis++;
        if(v.z > v.y) axis++;

        switch (axis) {
            case 0 -> v.x *= scale;
            case 1 -> v.y *= scale;
            case 2 -> v.z *= scale;
        }
    }




    public static Quat4f quat4(Quaternionf src) {
        return new Quat4f(src.x, src.y, src.z, src.w);
    }

    public static Matrix4f mat4f(javax.vecmath.Matrix4f src) {
        Matrix4f dst = new Matrix4f();
        dst.m00(src.m00);
        dst.m01(src.m01);
        dst.m02(src.m02);
        dst.m03(src.m03);

        dst.m10(src.m10);
        dst.m11(src.m11);
        dst.m12(src.m12);
        dst.m13(src.m13);

        dst.m20(src.m20);
        dst.m21(src.m21);
        dst.m22(src.m22);
        dst.m23(src.m23);

        dst.m30(src.m30);
        dst.m31(src.m31);
        dst.m32(src.m32);
        dst.m33(src.m33);

        return dst;
    }

    public static javax.vecmath.Matrix4f mat4f(Matrix4f src) {
        javax.vecmath.Matrix4f dst = new javax.vecmath.Matrix4f();
        dst.m00 = src.m00();
        dst.m01 = src.m01();
        dst.m02 = src.m02();
        dst.m03 = src.m03();

        dst.m10 = src.m10();
        dst.m11 = src.m11();
        dst.m12 = src.m12();
        dst.m13 = src.m13();

        dst.m20 = src.m20();
        dst.m21 = src.m21();
        dst.m22 = src.m22();
        dst.m23 = src.m23();

        dst.m30 = src.m30();
        dst.m31 = src.m31();
        dst.m32 = src.m32();
        dst.m33 = src.m33();

        return dst;

    }


}
