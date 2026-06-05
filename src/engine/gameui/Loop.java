package engine.gameui;

public class Loop {
    private UIPainter UIPainter;
    private Widget widget;

    public UIPainter getGfxPlatform() {
        return UIPainter;
    }

    public void setGfxPlatform(UIPainter UIPainter) {
        this.UIPainter = UIPainter;
    }

    public Widget getWidget() {
        return widget;
    }

    public void setWidget(Widget widget) {
        this.widget = widget;
    }

    public void update(int x, int y) {
        widget.update(UIPainter, x, y, widget.getRequiredWidth(), widget.getRequiredHeight());
    }
    public void update(int x, int y, int w, int h) {
        widget.update(UIPainter, x, y, w, h);
    }



}
