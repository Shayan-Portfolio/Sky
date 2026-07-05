package engine.ecs;

import engine.SystemState;

import java.util.List;

public class ScriptSystem extends ActorSystem {
    private Scene scene;

    public ScriptSystem(Scene scene) {
        this.scene = scene;
    }

    @Override
    public void run(Actor root) {
        if(SystemState.running) {
            root.previsitAllActors(actor -> {
                if (actor.has(ScriptComponent.class)) {
                    ScriptComponent scriptComponent = actor.getComponent(ScriptComponent.class);
                    if (!scriptComponent.script().initialized) {
                        scriptComponent.script().init(actor);
                        scriptComponent.script().initialized = true;
                    }

                    scriptComponent.script().update(actor, root);

                }
            });
        }
    }

    @Override
    public void dispose() {

    }
}
