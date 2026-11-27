package engine.ecs;

import engine.gameui.Loop;
import engine.gameui.Widget;
import engine.graphics.Rect2D;

@ComponentArray(mask = 1 << 10)
public class UIComponent implements Component {
    public Widget widget;
    public Loop loop;
    public Rect2D area;


    public UIComponent(Widget widget, Rect2D area) {
        this.widget = widget;
        this.area = area;
        this.loop = new Loop();
        this.loop.setWidget(widget);
    }

    @Override
    public void removed(Actor a) {}
}
