package engine.gameui;

import engine.graphics.Texture;

public class TextureBindings {
    public Texture[] textures = new Texture[32];
    public int cursor = 2;

    public int getTextureBinding(Texture texture) {
        for (int i = 0; i < textures.length; i++) {
            Texture t = textures[i];
            if (t == texture) {
                return i;
            }
        }

        int index = cursor;
        textures[cursor] = texture;
        cursor++;
        return index;
    }

}
