package engine.ecs;


public abstract class Script {
    private boolean active;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    public abstract void init(Actor actor);
    public abstract void update(Actor actor, Actor root);

    public void fixedUpdate(Actor actor, float timestep) {

    }
    public abstract boolean detach(Actor actor);
}
