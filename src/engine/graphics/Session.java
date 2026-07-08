package engine.graphics;

import engine.wsi.Surface;

public class Session {
    private static boolean isDiscrete;
    private static Surface surface;

    private Session(){}

    public static Surface getSurface() {
        return surface;
    }

    public static void setSurface(Surface surface) {
        Session.surface = surface;
    }

    public static boolean isDiscrete() {
        return isDiscrete;
    }

    public static void setDiscrete(boolean discrete) {
        isDiscrete = discrete;
    }
}
