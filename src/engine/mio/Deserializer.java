package engine.mio;

import engine.graphics.SceneRenderer;

import java.util.Iterator;

public interface Deserializer {
    Object deserialize(Iterator<Bytecode> iterator);
}
