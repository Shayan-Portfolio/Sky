package mio;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import engine.logging.SkyRuntimeException;
import engine.mio.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;

public class CodegenTests {
    public static void main(String[] args) {
        String source = """
                        MY_VALUE = "This \\"is a value"
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
            Codegen codegen = new Codegen();
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
        catch(ParseException e) {
            e.printStackTrace();
        }
    }
}
