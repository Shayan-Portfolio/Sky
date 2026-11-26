package engine.ecs;

import engine.graphics.Disposable;
import engine.logging.Logger;

public class Actor extends Disposable {
    private Component[] components = new Component[64];
    private long mask = 0L;
    private Actor[] children = new Actor[128];
    private int nextChildIndex;
    private String name;
    private Actor parent;

    public Actor(String name) {
        super(null);
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static void tryClassload(Class<?> clazz) {
        Logger.info(Actor.class, "Trying to load class: " + clazz.getSimpleName());
    }

    public Actor getParent() {
        return parent;
    }

    public Actor addActor(Actor... actor) {
        for(Actor a : actor) {
            addDisposable(a);
            a.parent = this;
            children[nextChildIndex++] = a;
        }

        return this;
    }

    public void removeActor(Actor actor) {
        for (int i = 0; i < children.length; i++) {
            if(children[i] == actor) {
                children[i] = null;
                removeDisposable(actor);
                actor.disposeAll();
            }
        }
    }

    public Actor[] getChildren() {
        return children;
    }

    public void previsitAllActors(ActorPreVisitor visitor) {
        visitor.visit(this);
        visitRecursive(visitor, this, true);
    }

    public void postvisitAllActors(ActorPostVisitor visitor) {
        visitRecursive(visitor, this, false);
        visitor.visit(this);
    }

    public static Actor newActor(String name, Component... components) {
        Actor actor = new Actor(name);
        for(Component component : components)
            actor.addComponent(component);

        return actor;
    }

    private void visitRecursive(ActorVisitor visitor, Actor actor, boolean pre) {
        Actor[] actors = actor.children;
        for (int i = 0; i < actors.length; i++) {
            Actor child = actors[i];

            if(child != null) {
                if(pre) visitor.visit(child);
                visitRecursive(visitor, child, pre);
                if(!pre) visitor.visit(child);
            }
        }
    }


    public void addComponent(Component component) {
        int index = getIndex(component.getClass());
        components[index] = component;
        mask |= getMask(component.getClass());
    }

    public boolean has(Class<? extends Component> clazz) {
        return (mask & getMask(clazz)) != 0;
    }

    public void removeComponent(Class<? extends Component> clazz) {
        int index = getIndex(clazz);
        components[index].removed(this);
        components[index] = null;
        mask = mask & ~getMask(clazz);
    }

    public <T extends Component> T getComponent(Class<T> c) {
        return ((T) components[getIndex(c)]);
    }

    public static long getMask(Class<? extends Component> clazz) {
        return clazz.getAnnotation(ComponentArray.class).mask();
    }

    public long getMask() {
        return mask;
    }

    public static int getIndex(Class<? extends Component> clazz) {
        return 64 - Long.numberOfTrailingZeros(getMask(clazz)) - 1;
    }

    @Override
    public void dispose() {

    }
}
