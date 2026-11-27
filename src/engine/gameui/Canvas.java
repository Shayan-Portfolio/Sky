package engine.gameui;

public abstract class Canvas extends Widget {
    private boolean ignore;
    private int width, height;

    public Canvas(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Widget setIgnore(boolean ignore) {
        this.ignore = ignore;
        return this;
    }

    @Override
    public int getRequiredWidth() {
        return width;
    }

    @Override
    public int getRequiredHeight() {
        return height;
    }

    @Override
    public void update(Paint paint, int x, int y, int w, int h) {
        if(!ignore)
            paint.drawRect(x, y, w, h, paint.getTheme().containerBackgroundColor);
        drawCustom(paint, x, y, w, h);
    }

    public abstract void drawCustom(Paint paint, int x, int y, int w, int h);
}
