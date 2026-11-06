package engine.gameui;

import engine.graphics.Texture;

public class TextureBindings {
    public Texture[][] textures;
    public int[] cursor = new int[]{2, 2};
    public int maxTextures = 32;

    public TextureBindings(int maxFramesInFlight) {
        this.textures = new Texture[maxFramesInFlight][maxTextures];
    }

    public int getTextureBinding(Texture texture, int frameIndex) {
        for (int i = 0; i < maxTextures; i++) {
            Texture t = textures[frameIndex][i];
            if (t == texture) {
                return i;
            }
        }
        return -1;
    }

    public int addBinding(Texture texture, int frameIndex) {
        int index = cursor[frameIndex];
        textures[frameIndex][cursor[frameIndex]] = texture;
        cursor[frameIndex]++;
        return index;
    }
}
