package engine.gameui;

public class Loop {
    private Paint paint;
    private Widget widget;

    public Widget getWidget() {
        return widget;
    }

    public void setWidget(Widget widget) {
        this.widget = widget;
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public void update(int x, int y) {
        widget.update(paint, x, y, widget.getRequiredWidth(), widget.getRequiredHeight());
    }
    public void update(int x, int y, int w, int h) {
        widget.update(paint, x, y, w, h);
    }



}
