package engine.ecs;

import engine.mujoco.MujocoMgr;

import java.lang.foreign.Arena;

public class MJSystem extends ActorSystem {
    private Arena arena;
    public MJSystem() {
        arena = MujocoMgr.getArena();


    }
    @Override
    public void run(Actor root) {
        root.previsitAllActors(actor -> {
            if(actor.has(MJComponent.class)) {
                MJComponent mj = actor.getComponent(MJComponent.class);







            }




        });

    }

    @Override
    public void dispose() {

    }
}
