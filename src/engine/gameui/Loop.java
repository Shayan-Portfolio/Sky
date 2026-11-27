package engine.gameui;

public class Loop {
    private Compositor compositor;
    private Widget widget;

    public Compositor getGfxPlatform() {
        return compositor;
    }

    public void setCompositor(Compositor compositor) {
        this.compositor = compositor;
    }

    public Widget getWidget() {
        return widget;
    }

    public void setWidget(Widget widget) {
        this.widget = widget;
    }

    public void update(int x, int y) {
        widget.update(compositor, x, y, widget.getRequiredWidth(), widget.getRequiredHeight());
    }
    public void update(int x, int y, int w, int h) {
        widget.update(compositor, x, y, w, h);
    }



}
