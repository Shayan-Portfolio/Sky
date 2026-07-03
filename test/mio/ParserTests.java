package mio;

import engine.mio.Analyzer;
import engine.mio.ParseException;
import engine.mio.RecursiveDescentParser;

class ParserTests {
    public static void main(String[] args) {
        System.out.println(testMissingToken());
        System.out.println(testMissingParameter());
    }

    private static boolean testMissingParameter() {
        String source = """
                        #This is a comment
                        MY_VALUE = "This is a value"
                        
                        #This is Mio, the scene file format in Sky
                        
                        actor Foo
                            add Bar
                                MyString = "Foo"
                                MyTypedInt = int()
                                MyWeakInt = vec2(0, 0)
                            end
                        end
                        """;

        Analyzer tokenizer = new Analyzer(source);

        RecursiveDescentParser parser = new RecursiveDescentParser();

        try {
            parser.parseAll(tokenizer);
        }
        catch(ParseException e) {
            return e.getMessage().equals("Expected any of [Numeric] next instead of RParen () on line 11");
        }
        return false;
    }
    private static boolean testMissingToken() {
        String source = """
                        #This is a comment
                        MY_VALUE = "This is a value"
                        
                        #This is Mio, the scene file format in Sky
                        
                        actor Foo
                            add Bar
                                MyString = "Foo"
                                MyTypedInt int(0)
                                MyWeakInt = vec2(0, 0)
                            end
                        end
                        """;

        Analyzer tokenizer = new Analyzer(source);

        RecursiveDescentParser parser = new RecursiveDescentParser();

        try {
            parser.parseAll(tokenizer);
        }
        catch(ParseException e) {
            return e.getMessage().equals("Expected any of [Equals] next instead of Int1Keyword (int) on line 11");
        }
        return false;
    }
}