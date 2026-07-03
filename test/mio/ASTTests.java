package mio;

import engine.mio.ASTNode;
import engine.mio.Analyzer;
import engine.mio.ParseException;
import engine.mio.RecursiveDescentParser;

public class ASTTests {
    public static void main(String[] args) {
        String source = """
                        #This is a comment
                        MY_VALUE = "This is a value"
                        
                        #This is Mio, the scene file format in Sky
                        
                        actor Foo
                            add Bar
                                MyString = "Foo"
                                MyTypedInt = 34
                            end
                        end
                        """;

        Analyzer tokenizer = new Analyzer(source);

        RecursiveDescentParser parser = new RecursiveDescentParser();

        try {
            ASTNode node = parser.parseAll(tokenizer);
            System.out.println();
        }
        catch(ParseException e) {
            e.printStackTrace();
        }
    }
}
