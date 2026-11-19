package tests.box3d;

import java.lang.foreign.*;
import static box3d.box3d_h.*;

import box3d.*;
import org.lwjgl.system.Library;

public class Main {



    static void main() {
        Library.loadSystem("box3d", "natives/box3d");

        Arena arena = Arena.ofConfined();

        var version = b3GetVersion(arena);

        int major = b3Version.major(version);
        int minor = b3Version.minor(version);


        System.out.println("Box3D " + major + " " + minor);

        var worldDef = b3DefaultWorldDef(arena);

        var g = arena.allocate(b3Vec3.layout());
        {
            b3Vec3.x(g, 0);
            b3Vec3.y(g, -10f);
            b3Vec3.z(g, 0);
        }
        b3WorldDef.gravity(worldDef, g);


        var world = b3CreateWorld(arena, worldDef);


        //Floor
        var groundPosition = arena.allocate(b3Vec3.layout());
        {
            b3Vec3.x(groundPosition, 0);
            b3Vec3.y(groundPosition, 0);
            b3Vec3.z(groundPosition, 0);
        }

        var groundBodyDef = b3DefaultBodyDef(arena);
        b3BodyDef.position(groundBodyDef, groundPosition);

        var groundBody = b3CreateBody(arena, world, groundBodyDef);
        var groundBox = b3MakeBoxHull(arena, 50f, 10f, 50f);
        var groundShapeDef = b3DefaultShapeDef(arena);
        b3CreateHullShape(arena, groundBody, groundShapeDef, b3BoxHull.base(groundBox));
        //Floor End


        //Dynamic Body
        var p = arena.allocate(b3Vec3.layout());
        {
            b3Vec3.x(p, 0);
            b3Vec3.y(p, 4);
            b3Vec3.z(p, 0);
        }

        var bodyDef = b3DefaultBodyDef(arena);
        b3BodyDef.type(bodyDef, b3_dynamicBody());

        var body = b3CreateBody(arena, world, bodyDef);
        var box = b3MakeCubeHull(arena, 1.0f);
        var shapeDef = b3DefaultShapeDef(arena);
        b3ShapeDef.density(shapeDef, 1.0f);
        b3CreateHullShape(arena, body, shapeDef, b3BoxHull.base(box));
        //Dynamic Body End


        float timeStep = 1.0f / 60.0f;
        int subStepCount = 4;


        for (int i = 0; i < 90; ++i) {
            b3World_Step(world, timeStep, subStepCount);

            var pos = b3Body_GetPosition(arena, body);
            float x = b3Vec3.x(pos);
            float y = b3Vec3.y(pos);
            float z = b3Vec3.z(pos);

            System.out.println(x + " " + y + " " + z);


        }

        b3DestroyBody(groundBody);

        b3DestroyWorld(world);
    }
}
