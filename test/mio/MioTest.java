package mio;

import engine.logging.Logger;
import engine.mio.SceneCompiler;

class MioTest {
    public static void main(String[] args) {
        Logger.setConsoleTarget(System.out);
        SceneCompiler.compile(
                """
                        #This is a comment
                        MY_VALUE = "This is a value"
                        
                        #This is Mio, the scene file format in Sky
                        
                        actor Foo
                            add Bar
                                MyString = "Foo"
                                MyTypedInt = int(0.0)
                                MyWeakInt = vec2(0, 0)
                            end
                        end
                        """);


    }
}