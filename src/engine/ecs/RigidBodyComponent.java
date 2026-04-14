package engine.ecs;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MotionState;
import engine.physics.Collider;
import engine.physics.BodyParams;

@ComponentArray(mask = 1 << 4)
public class RigidBodyComponent {
    public RigidBody rigidBody;
    public Collider collider;
    public MotionState motionState;
    public RigidBodyConstructionInfo constructionInfo;
    public boolean active;
    public float mass = 1;
    public BodyParams mat;
    public boolean rotate = true;

    public RigidBodyComponent(Collider collider, float mass, BodyParams mat) {
        this.collider = collider;
        this.mass = mass;
        this.mat = mat;
    }

    public RigidBodyComponent(Collider collider, float mass, BodyParams mat, boolean rotate) {
        this.collider = collider;
        this.mass = mass;
        this.mat = mat;
        this.rotate = rotate;
    }


}
