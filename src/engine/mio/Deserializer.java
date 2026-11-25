package engine.mio;

import engine.ecs.Component;
import engine.graphics.SceneRenderer;

import java.util.Iterator;

public interface Deserializer {
    Component deserialize(Iterator<Bytecode> iterator);
}
