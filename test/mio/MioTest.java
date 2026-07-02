package mio;

import engine.mio.SceneCompiler;

class MioTest {
    public static void main(String[] args) {
        SceneCompiler.printTokens(
                """
                #This is a comment
                let MY_VALUE = 32
                uses "stdlib.mi"
                actor Foo
                    add Bar
                        MyString = "Foo",
                        MyTypedInt = int(0),
                        MyWeakInt = 0
                    
                    end
                end
                
                
                """);


    }
}