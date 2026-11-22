package engine.mujoco;

import java.lang.foreign.Arena;

public class MujocoMgr {
    private MujocoMgr() {}
    private static Arena ARENA;

    public static Arena getArena() {
        if(ARENA == null) ARENA = Arena.ofConfined();
        return ARENA;
    }

}
