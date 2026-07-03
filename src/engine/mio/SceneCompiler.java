package engine.mio;

public class SceneCompiler {
    private SceneCompiler() {}

    public static SceneBytecode compile(String source) {
        Analyzer tokenizer = new Analyzer(source);

        RecursiveDescentParser parser = new RecursiveDescentParser();
        parser.parseAll(tokenizer);

        return parser.getEmittedBytecode();
    }

    public static void printTokens(String s) {
        Analyzer tokenizer = new Analyzer(s);
        Analyzer.Token t = null;

        while((t = tokenizer.next()) != null) {
            System.out.println(t.type + "| " + t.content.toString());
        }

    }
}
