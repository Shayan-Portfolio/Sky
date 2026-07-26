package engine.asset;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import engine.vfs.FileSystem;
import engine.logging.Logger;
import engine.logging.SkyRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static org.lwjgl.system.MemoryStack.stackPush;

public class AssetPackage {

    private String namespace;
    private HashMap<String, Asset> assetMap = new HashMap<>();

    public AssetPackage(String namespace, HashMap<String, Asset> assetMap) {
        this.namespace = namespace;
        this.assetMap = assetMap;
    }

    public AssetPackage() {}

    private static String useForwardSlash(String path) {
        return path.replace("\\", "/");
    }

    private static String useNativeSlash(String path) {
        return path.replace("/", File.separator);
    }

    public static AssetPackage openLocal(String namespace, Path path) {
        HashMap<String, Asset> assetMap = new HashMap<>();
        AssetPackage assetPackage = new AssetPackage(namespace, assetMap);

        {
            try {
                Files.walkFileTree(path, new FileVisitor<>() {

                    @NotNull
                    @Override
                    public FileVisitResult visitFile(Path assetPath, @NotNull BasicFileAttributes attrs) {
                        String assetFilePath = assetPath.toString();
                        String identifier = useForwardSlash(assetFilePath);

                        Object asset = loadRes(identifier, assetPath, namespace);
                        assetMap.put(identifier, new Asset(assetPackage, identifier, asset));
                        return FileVisitResult.CONTINUE;
                    }

                    @NotNull
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) {
                        if(dir.toString().endsWith("src")) return FileVisitResult.SKIP_SUBTREE;
                        return FileVisitResult.CONTINUE;
                    }

                    @NotNull
                    @Override
                    public FileVisitResult visitFileFailed(Path file, @NotNull IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }

                    @NotNull
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            catch (IOException e) {
                throw new SkyRuntimeException(e);
            }
        }

        return assetPackage;
    }

    public static Object loadRes(String identifier, Path assetPath, String namespace) {
        String assetFilePath = useNativeSlash(assetPath.toString());
        Object object = null;
        try(MemoryStack stack = stackPush()) {
            {
                if (assetFilePath.endsWith("png") ||
                        assetFilePath.endsWith("jpg") ||
                        assetFilePath.endsWith("jpeg")) {


                    IntBuffer w = stack.callocInt(1);
                    IntBuffer h = stack.callocInt(1);
                    IntBuffer channelsInFile = stack.callocInt(1);
                    ByteBuffer texture = STBImage.stbi_load(
                            assetFilePath,
                            w,
                            h,
                            channelsInFile,
                            4
                    );
                    Logger.info(AssetPackage.class, "STBImage says \"" + STBImage.stbi_failure_reason() + "\" for " + identifier);
                    int size = texture.remaining();
                    byte[] bytes = new byte[texture.remaining()];

                    texture.limit(size);
                    texture.get(bytes);
                    texture.limit(texture.capacity()).rewind();

                    object = new TextureData(bytes, w.get(0), h.get(0));
                    MemoryUtil.memFree(texture);
                } else if (assetFilePath.endsWith("spv")) {
                    object = FileSystem.readBytes(assetPath);
                } else if (assetFilePath.endsWith("wav") || assetFilePath.endsWith("bin")) {
                    object = FileSystem.readBytes(assetPath);

                } else if (assetFilePath.endsWith("json") || assetFilePath.endsWith("gltf") || assetFilePath.endsWith("scene")) {
                    object = FileSystem.readString(assetPath);
                }

                //assetMap.put(identifier, asset);
                Logger.info(AssetPackage.class, "Loading asset " + identifier + " into namespace " + namespace);
            }
        }
        return object;
    }

    public static AssetPackage openPackage(String namespace, Path path) {
        Kryo kryo = newKryo();

        Input input;
        try {
            input = new Input(new InflaterInputStream(Files.newInputStream(path)));
        }
        catch (IOException e) {
            throw new SkyRuntimeException(e);
        }
        HashMap<String, Asset> assetMap = kryo.readObject(input, HashMap.class);
        input.close();

        return new AssetPackage(namespace, assetMap);
    }

    public static void createPackage(Path path, AssetPackage assetPackage) {
        Kryo kryo = newKryo();

        Output output;
        try {
            output = new Output(new DeflaterOutputStream(Files.newOutputStream(path)));
        }
        catch (IOException e) {
            throw new SkyRuntimeException(e);
        }

        kryo.writeObject(output, assetPackage.getAssetMap());

        output.flush();
        output.close();
    }

    private static Kryo newKryo() {
        Kryo kryo = new Kryo();
        kryo.setReferences(true);
        kryo.register(HashMap.class);
        kryo.register(Asset.class);
        kryo.register(AssetPackage.class);
        kryo.register(byte[].class);
        kryo.register(TextureData.class);

        return kryo;
    }

    public HashMap<String, Asset> getAssetMap() {
        return assetMap;
    }
    protected <T> Asset<T> getAsset(String assetIdentifier) {
        return assetMap.get(assetIdentifier);
    }

    public String getNamespace() {
        return namespace;
    }
}
