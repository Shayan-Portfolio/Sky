package engine.ecs.compiler;

import engine.ecs.Component;

import java.util.Iterator;

public interface Deserializer {
    Component deserialize(Iterator<Bytecode> iterator);
}
