package mio;

import engine.mio.*;

public class ASTTests {
    public static void main(String[] args) {
        Context source = new Context("test.scene", """
                        #This is a comment
                        MY_VALUE = "This is a value"
                        
                        #This is Mio, the scene file format in Sky
                        
                        actor Foo
                            add Bar
                                MyString = "Foo"
                                MyTypedInt = 34
                            end
                        end
                        """, new DefaultLogStrategy());

        Analyzer tokenizer = new Analyzer(source);

        RecursiveDescentParser parser = new RecursiveDescentParser(source);

        try {
            ASTNode node = parser.parseAll(tokenizer);
            System.out.println();
        }
        catch(ParseException e) {
            e.printStackTrace();
        }
    }
}
