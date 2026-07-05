package engine.physics;

import com.bulletphysics.collision.shapes.*;
import engine.graphics.Disposable;

import javax.vecmath.Vector3f;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

public class Collider {
    private CollisionShape collisionShape;

    private Collider(CollisionShape collisionShape) {
        this.collisionShape = collisionShape;
    }

    public static Collider newCylinderCollider(float radius, float height) {
        return new Collider(new CylinderShape(new Vector3f(radius, height / 2, radius)));
    }

    public CollisionShape getCollisionShape() {
        return collisionShape;
    }

    public static Collider newBoxCollider(float w, float h, float d) {
        return new Collider(new BoxShape(new Vector3f(w / 2, h / 2, d / 2)));
    }

    private static void put(List<Float> data, FloatBuffer floatBuffer) {
        for(float f : data) {
            floatBuffer.put(f);
        }
    }

    private static void put(List<Integer> data, IntBuffer intBuffer) {
        for(int f : data) {
            intBuffer.put(f);
        }
    }

    public static Collider newTriangleMeshCollider(Disposable parent, List<Float> positions, List<Integer> indices) {

        TriangleIndexVertexArray triangleIndexVertexArray = new TriangleIndexVertexArray();
        IndexedMesh indexedMesh = new IndexedMesh();

        {
            ByteBuffer indexBuffer = ByteBuffer.allocate(indices.size() * Integer.BYTES);
            IntBuffer intIndexBuffer = indexBuffer.asIntBuffer();
            put(indices, intIndexBuffer);
            indexedMesh.triangleIndexBase = indexBuffer;
            indexedMesh.triangleIndexStride = 3 * Integer.BYTES;
            indexedMesh.numTriangles = indices.size() / 3;

            ByteBuffer pPositions = ByteBuffer.allocate(positions.size() * Float.BYTES);
            FloatBuffer fPositions = pPositions.asFloatBuffer();
            put(positions, fPositions);
            indexedMesh.vertexBase = pPositions;
            indexedMesh.vertexStride = 3 * Float.BYTES;
            indexedMesh.numVertices = positions.size() / 3;
        }

        triangleIndexVertexArray.addIndexedMesh(indexedMesh);


        BvhTriangleMeshShape bvhTriangleMeshShape = new BvhTriangleMeshShape(triangleIndexVertexArray, true);
        return new Collider(bvhTriangleMeshShape);
    }

    public static Collider newSphereCollider(float r) {
        return new Collider(new SphereShape(r));
    }
}
