package engine.gameui;

public abstract class LayoutEngine {
    protected Widget widget;

    public Widget getWidget() {
        return widget;
    }

    public void setWidget(Widget widget) {
        this.widget = widget;
    }

    public abstract int getComputedWidth();
    public abstract int getComputedHeight();

    public abstract void updateChildren(UIPainter platform, int x, int y, int w, int h);
}
