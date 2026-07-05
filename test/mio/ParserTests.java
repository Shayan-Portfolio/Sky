package mio;

import engine.mio.*;

class ParserTests {
    public static void main(String[] args) {
        System.out.println(testMissingToken());
        System.out.println(testMissingParameter());
    }

    private static boolean testMissingParameter() {
        Context source = new Context("test.scene", """
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
                        """, new DefaultLogStrategy());

        Analyzer tokenizer = new Analyzer(source);

        RecursiveDescentParser parser = new RecursiveDescentParser(source);

        try {
            parser.parseAll(tokenizer);
        }
        catch(ParseException e) {
            return e.getMessage().equals("Expected any of [Numeric] next instead of RParen () on line 9");
        }
        return false;
    }
    private static boolean testMissingToken() {
        Context source = new Context("test.scene", """
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
                        """, new DefaultLogStrategy());

        Analyzer tokenizer = new Analyzer(source);

        RecursiveDescentParser parser = new RecursiveDescentParser(source);

        try {
            parser.parseAll(tokenizer);
        }
        catch(ParseException e) {
            return e.getMessage().equals("Expected any of [Equals] next instead of Int (int) on line 9");
        }
        return false;
    }
}