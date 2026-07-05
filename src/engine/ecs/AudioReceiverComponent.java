package engine.ecs;


import org.joml.Vector3f;

@ComponentArray(mask = 1 << 10)
public record AudioReceiverComponent(Vector3f position, Vector3f direction) {}
