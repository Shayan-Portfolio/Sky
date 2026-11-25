package engine.ecs;


import org.joml.Vector3f;

@ComponentArray(mask = 1)
public record AudioReceiverComponent(Vector3f position, Vector3f direction) implements Component {

    @Override
    public void removed(Actor a) {}
}
