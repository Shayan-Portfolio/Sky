package engine.ecs;

public class ScriptSystem extends ActorSystem {

    @Override
    public void run(Actor root) {
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

    @Override
    public void dispose() {

    }
}
