package engine.gltf2;

import com.google.gson.GsonBuilder;
import engine.gltf2.schemas.Gltf;

public class Loader {
    public Gltf parse(Source src) {
        return new GsonBuilder().create().fromJson(src.gltf().getObject(), Gltf.class);
    }
}
