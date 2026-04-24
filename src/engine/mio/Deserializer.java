package engine.mio;

import engine.graphics.Renderer;

import java.util.Iterator;

public interface Deserializer {
    Object deserialize(Iterator<Instruction> iterator, Renderer renderer);
}
