package engine.gameui;

import engine.graphics.Color;

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
    public void update(UIPainter platform, int x, int y, int w, int h) {
        if(!ignore)
            platform.drawRect(x, y, w, h, platform.getTheme().containerBackgroundColor);
        updateChildren(platform, x + padding, y + padding, w - padding, h - padding);
    }



}
