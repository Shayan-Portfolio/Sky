package ffm;

import java.lang.foreign.*;
import static box3d.box3d_h.*;
import org.lwjgl.system.Library;

public class Main {
    static void main() {
        Library.loadSystem("box3d", "natives/box3d");

        Arena arena = Arena.ofConfined();

        var worldDef = b3DefaultWorldDef(arena);
        var world = b3CreateWorld(arena, worldDef);

        b3DestroyWorld(world);
    }
}
