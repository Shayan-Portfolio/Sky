package mio;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import engine.logging.SkyRuntimeException;
import engine.mio.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;

public class SATests {
    public static void main(String[] args) {
        Context source = new Context("test.scene",
                """
                actor Foo
                    add Bar
                        Foo = 1
                        Foo = 2
                    end
                end
                """, new DefaultLogStrategy());









        Analyzer tokenizer = new Analyzer(source);

        RecursiveDescentParser parser = new RecursiveDescentParser(source);

        try {
            ASTNode node = parser.parseAll(tokenizer);
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(source);
            semanticAnalyzer.analyze(node);
            Codegen codegen = new Codegen(source);
            BytecodeStream stream = codegen.flatten(node);

            {
                Kryo kryo = new Kryo();
                kryo.setRegistrationRequired(false);

                Output output;
                try {
                    output = new Output(new DeflaterOutputStream(Files.newOutputStream(Path.of("output.mi"))));
                }
                catch (IOException e) {
                    throw new SkyRuntimeException(e);
                }

                kryo.writeObject(output, stream);

                output.flush();
                output.close();
            }
        }
        catch(ParseException | SemanticAnalysisException e) {
            e.printStackTrace();
        }
    }
}
