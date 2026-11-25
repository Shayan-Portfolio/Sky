package engine.ecs;

import engine.graphics.Material;
import engine.graphics.Texture;

@ComponentArray(mask = 1 << 4)
public class MaterialComponent implements Component {
    public Material material;

    public MaterialComponent(Material material) {
        this.material = material;
    }

    @Override
    public void removed(Actor a) {
        for(Texture texture : material.getTextures()) {
            texture.disposeAll();
            a.removeDisposable(texture);
        }
        material.getSampler().disposeAll();
        a.removeDisposable(material.getSampler());
    }
}
