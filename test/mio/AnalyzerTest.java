package mio;

import engine.mio.Analyzer;
import engine.mio.Context;
import engine.mio.DefaultLogStrategy;

class AnalyzerTest {
    public static void main(String[] args) {
        Context src = new Context("test.scene", """
                MY_VALUE = "This \\"is a value"
                actor Foo
                    add Bar
                        MyString = "Foo"
                        MyTypedInt = int(34)
                    end
                
                    add Baz
                        MyFoo = "A"
                        MyBar = vec3(1, 2, 3)
                
                    end
                end
                        
                        """, new DefaultLogStrategy());

        Analyzer tokenizer = new Analyzer(src);
        Analyzer.Token t = null;

        while((t = tokenizer.next()) != null) {
            System.out.println(t.type + "| " + t.content.toString());
        }

        System.out.println(testStringEscape());
    }

    private static boolean testStringEscape() {
        Context src = new Context("test.scene", """
                        #This is a comment
                        MY_VALUE = "This \\"is a value"
                        
                        """, new DefaultLogStrategy());

        Analyzer tokenizer = new Analyzer(src);
        Analyzer.Token t = null;
        int index = 0;
        String[] tokenContents = new String[3];

        while((t = tokenizer.next()) != null) {
            tokenContents[index++] = t.content.toString();
        }

        return tokenContents[0].equals("MY_VALUE") && tokenContents[1].equals("") && tokenContents[2].equals("This \"is a value");
    }


}