package engine.ecs;


import engine.asset.Asset;
import engine.asset.AssetListener;
import engine.asset.AssetRegistry;
import engine.ecs.std.StdDeserializers;
import engine.graphics.*;
import engine.logging.Logger;
import engine.logging.SkyRuntimeException;
import engine.mio.*;
import engine.util.ExceptionUtil;

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


    public void load(Asset<String> src) {

        Actor top = Actor.newActor(src.getFQN());
        actor.addActor(top);
        actor = top;

        Context ctx = new Context(
                src.getFQN(),
                src.getObject(),
                new DefaultLogStrategy()
        );

        try {
            BytecodeStream bytecodeStream = Mio.compile(ctx);
            load(bytecodeStream);
        }
        catch (CompilerException e) {
            throw new SkyRuntimeException(e);
        }

        src.addListener("reload_source", () -> {
            Context context = new Context(
                    src.getFQN(),
                    src.getObject(),
                    new DefaultLogStrategy()
            );


            try {
                BytecodeStream bytecodeStream = Mio.compile(context);

                for(Actor c : top.getChildren()) {
                    if(c == null) continue;
                    top.removeActor(c);
                }
                actor = top;


                load(bytecodeStream);
            } catch (CompilerException e) {
                Logger.error(Scene.class, "Compile failed");
                Logger.error(Scene.class, ExceptionUtil.exceptionToString(e));
            }

        });

    }

    private void load(BytecodeStream bytecodeStream) {
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
                    Component data = deserializer.deserialize(iterator);
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
