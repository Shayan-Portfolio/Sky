package engine.gameui;

public class Panel extends Widget {
    private boolean ignore;

    public Widget setIgnore(boolean ignore) {
        this.ignore = ignore;
        return this;
    }

    @Override
    public int getRequiredWidth() {
        return layoutEngine.getComputedWidth() + (padding * 2);
    }

    @Override
    public int getRequiredHeight() {
        return layoutEngine.getComputedHeight() + (padding * 2);
    }

    @Override
    public void update(Paint paint, int x, int y, int w, int h) {
        if(!ignore)
            paint.drawRect(x, y, w, h, paint.getTheme().containerBackgroundColor);
        updateChildren(paint, x + padding, y + padding, w - padding, h - padding);
    }



}
