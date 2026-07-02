package mio;

import engine.mio.SceneCompiler;

class MioTest {
    public static void main(String[] args) {
        SceneCompiler.printTokens("""
                
                
                actor MyActor 
                    add Transform
                        "key" = vec4(1.0, 2.0, 3.0, 4.0)
                        
                    end
                end
                
                
                """);


    }
}