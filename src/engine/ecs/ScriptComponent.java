package engine.ecs;

@ComponentArray(mask = 1 << 7)
public record ScriptComponent(Script script) implements Component {
    @Override
    public void removed(Actor a) {}
}
