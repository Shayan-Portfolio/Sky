package engine.ecs;

public class ScriptSystem extends ActorSystem {

    @Override
    public void run(Actor root) {
        root.previsitAllActors(actor -> {
            if (actor.has(ScriptComponent.class)) {
                ScriptComponent scriptComponent = actor.getComponent(ScriptComponent.class);
                if (!scriptComponent.script().isActive()) {
                    scriptComponent.script().init(actor);
                    scriptComponent.script().setActive(true);
                }

                scriptComponent.script().update(actor, root);

            }
        });
    }

    @Override
    public void dispose() {

    }
}
