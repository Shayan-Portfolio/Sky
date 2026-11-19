package engine.ecs;

public abstract class ActorSystem {
    public abstract void run(Actor root);
    public abstract void dispose();
}
