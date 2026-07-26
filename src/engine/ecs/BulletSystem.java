package engine.ecs;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import engine.Time;
import engine.physics.Physics;
import engine.util.MathUtil;
import org.joml.Quaternionf;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

public class BulletSystem extends ActorSystem {
    private DiscreteDynamicsWorld dynamicsWorld;

    //A few variables to avoid reallocating objects (VM -> vecmath, JML -> JOML)
    private Transform transformVM = new Transform();
    private Quat4f rotVM = new Quat4f();
    private Quaternionf rotJML = new Quaternionf();
    private Matrix4f posVM = new Matrix4f();
    private float accumulator = 0;
    private float timestep = 1 / 60f;

    public BulletSystem() {
        BroadphaseInterface broadphaseInterface = new DbvtBroadphase();
        CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        CollisionDispatcher collisionDispatcher = new CollisionDispatcher(collisionConfiguration);
        ConstraintSolver constraintSolver = new SequentialImpulseConstraintSolver();


        dynamicsWorld = new DiscreteDynamicsWorld(collisionDispatcher, broadphaseInterface, constraintSolver, collisionConfiguration);
        dynamicsWorld.setGravity(new Vector3f(0, -9.81f, 0));
        Physics.world = dynamicsWorld;
    }

    @Override
    public void run(Actor root) {
        root.previsitAllActors(actor -> {
            if (actor.has(RigidBodyComponent.class)) {
                RigidBodyComponent rigidBodyComponent = actor.getComponent(RigidBodyComponent.class);
                TransformComponent transformComponent = actor.getComponent(TransformComponent.class);

                if (!rigidBodyComponent.active) {

                    org.joml.Vector3f pos = new org.joml.Vector3f(
                            transformComponent.transform().m30(),
                            transformComponent.transform().m31(),
                            transformComponent.transform().m32()
                    );

                    Quaternionf rotation = new Quaternionf();
                    {
                        transformComponent.transform().getNormalizedRotation(rotation);
                    }


                    rigidBodyComponent.motionState = new DefaultMotionState(new Transform(new Matrix4f(
                            MathUtil.quat4(rotation),
                            MathUtil.vec3(pos),
                            1.0f
                    )));

                    Vector3f inertia = new Vector3f(0, 0, 0);
                    rigidBodyComponent.collider.getCollisionShape().calculateLocalInertia(rigidBodyComponent.mass, inertia);
                    rigidBodyComponent.constructionInfo = new RigidBodyConstructionInfo(
                            rigidBodyComponent.mass,
                            rigidBodyComponent.motionState,
                            rigidBodyComponent.collider.getCollisionShape(),
                            inertia
                    );
                    rigidBodyComponent.constructionInfo.friction = rigidBodyComponent.mat.friction();
                    rigidBodyComponent.constructionInfo.restitution = rigidBodyComponent.mat.restitution();


                    rigidBodyComponent.rigidBody = new RigidBody(rigidBodyComponent.constructionInfo);
                    rigidBodyComponent.rigidBody.setUserPointer(actor);
                    rigidBodyComponent.rigidBody.setAngularFactor(rigidBodyComponent.rotate ? 1f : 0f);

                    dynamicsWorld.addRigidBody(rigidBodyComponent.rigidBody);


                    rigidBodyComponent.active = true;
                } else {
                    transformVM.setIdentity();
                    rigidBodyComponent.rigidBody.getWorldTransform(transformVM);

                    posVM.setIdentity();
                    transformVM.getMatrix(posVM);
                    transformVM.getRotation(rotVM);

                    MathUtil.copy(rotVM, rotJML);

                    float x = posVM.m03;
                    float y = posVM.m13;
                    float z = posVM.m23;

                    transformComponent
                            .transform()
                            .identity()
                            .translate(x, y, z)
                            .rotate(rotJML);

                }


            }
            if (actor.has(ScriptComponent.class))
                actor.getComponent(ScriptComponent.class).script().fixedUpdate(actor, timestep);


        });
        dynamicsWorld.stepSimulation(Time.deltaTime(), 20, timestep);

    }

    @Override
    public void dispose() {

    }
}
