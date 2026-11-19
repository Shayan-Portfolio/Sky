package engine.ecs;

import engine.graphics.Disposable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Engine extends Disposable {
    private List<ActorSystem> systems = new ArrayList<>();

    public Engine(Disposable parent) {
        super(parent);
    }

    public void update(Scene scene) {
        for(ActorSystem system : systems) {
            system.run(scene.getRootActor());
        }
    }




    public void addSystem(ActorSystem... actorSystems) {
        this.systems.addAll(Arrays.asList(actorSystems));
    }


    public void removeSystem(ActorSystem actorSystem) {
        systems.remove(actorSystem);
    }


    public void close() {
        for(ActorSystem system : systems) {
            system.dispose();
        }
    }

    @Override
    public void dispose() {

    }
}
