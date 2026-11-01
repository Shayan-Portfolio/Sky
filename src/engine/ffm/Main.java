package engine.ffm;

import java.io.File;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class Main {
    static void main() throws Throwable {
        System.load(new File("ffm/hello_sky_ffm.dll").getAbsolutePath());

        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = SymbolLookup.loaderLookup();

        MethodHandle hello_sky = linker.downcallHandle(
                lookup.find("hello").orElseThrow(),
                FunctionDescriptor.ofVoid()
        );

        hello_sky.invoke();


    }
}
