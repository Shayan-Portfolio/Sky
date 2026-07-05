package engine.mio;

import engine.graphics.Renderer;

import java.util.Iterator;

public interface Deserializer {
    Object deserialize(Iterator<Bytecode> iterator, Renderer renderer);
}
