package mio;

import engine.mio.Analyzer;
import engine.mio.ParseException;
import engine.mio.RecursiveDescentParser;
import engine.mio.SceneCompiler;

class AnalyzerTest {
    public static void main(String[] args) {
        String src = """
                        #This is a comment
                        MY_VALUE = 0.0.0
                        
                        """;

        Analyzer tokenizer = new Analyzer(src);
        Analyzer.Token t = null;

        while((t = tokenizer.next()) != null) {
            System.out.println(t.type + "| " + t.content.toString());
        }

        System.out.println(testStringEscape());
    }

    private static boolean testStringEscape() {
        String src = """
                        #This is a comment
                        MY_VALUE = "This \\"is a value"
                        
                        """;

        Analyzer tokenizer = new Analyzer(src);
        Analyzer.Token t = null;
        int index = 0;
        String[] tokenContents = new String[3];

        while((t = tokenizer.next()) != null) {
            tokenContents[index++] = t.content.toString();
        }

        return tokenContents[0].equals("MY_VALUE") && tokenContents[1].equals("") && tokenContents[2].equals("\"This \"is a value\"");
    }


}