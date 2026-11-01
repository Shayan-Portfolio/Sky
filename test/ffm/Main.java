package ffm;

import java.io.File;
import java.lang.foreign.*;
import static box3d.box3d_h.*;
import org.lwjgl.system.Platform;

public class Main {
    static void main() {
        if(Platform.get() != Platform.WINDOWS) throw new RuntimeException("box3d is only supported on Windows for now");
        System.load(new File("thirdparty/box3d.dll").getAbsolutePath());
        Arena arena = Arena.ofConfined();

        var worldDef = b3DefaultWorldDef(arena);
        var world = b3CreateWorld(arena, worldDef);

        b3DestroyWorld(world);
    }
}
