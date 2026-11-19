package engine.ecs;


import engine.ecs.std.StdDeserializers;
import engine.graphics.*;
import engine.logging.SkyRuntimeException;
import engine.mio.Deserializer;
import engine.mio.BytecodeStream;
import engine.mio.Bytecode;

import java.util.*;

public class Scene extends Disposable {
    private Actor root;
    private String name;
    private Actor actor;
    private HashMap<String, Deserializer> deserializers = new HashMap<>();



    public Scene(Disposable parent, String name) {
        super(parent);
        this.name = name;

        StdDeserializers.install(this);
    }

    public void registerDeserializer(String clazz, Deserializer deserializer) {
        deserializers.put(clazz, deserializer);
    }





    public Actor getRootActor() {
        return root;
    }

    public void setRootActor(Actor root) {
        this.root = root;
        this.actor = root;
        addDisposable(this.actor);
    }

    public Actor getActorBeingLoaded() {
        return actor;
    }


    public void load(BytecodeStream bytecodeStream) {

        for (Iterator<Bytecode> iterator = bytecodeStream.getList().iterator(); iterator.hasNext(); ) {
            Bytecode bytecode = iterator.next();
            switch (bytecode.opcode()) {
                case BeginActor -> {
                    String actorName = (String) bytecode.operands()[0];
                    Actor child = new Actor(actorName);
                    if (actor != null) actor.addActor(child);
                    actor = child;
                    break;
                }
                case BeginAdd -> {
                    Deserializer deserializer = deserializers.getOrDefault((String) bytecode.operands()[0], null);
                    if (deserializer == null) throw new SkyRuntimeException("No ECS deserializer for " + bytecode.operands()[0]);
                    Object data = deserializer.deserialize(iterator);
                    actor.addComponent(data);
                    break;
                }
                case EndActor -> {
                    actor = actor.getParent();
                    break;
                }
            }

        }


    }

    public Actor newRootActor(String name) {
        return new Actor(name);
    }



    @Override
    public void dispose() {

    }
}
