package engine.gameui;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class Widget {
    private List<Widget> widgets = new LinkedList<>();
    private List<EventHandler> eventHandlers = new LinkedList<>();
    private long hints;
    private String name;
    protected int padding = 4;
    protected boolean focused;
    protected Widget parent;
    protected Map<String, Object> properties = new HashMap<>();
    protected LayoutEngine layoutEngine = new LayoutEngine() {
        @Override
        public int getComputedWidth() {
            return 0;
        }

        @Override
        public int getComputedHeight() {
            return 0;
        }

        @Override
        public void updateChildren(UIPainter platform, int x, int y, int w, int h) {

        }
    };

    public int getPadding() {
        return padding;
    }

    public Widget setPadding(int padding) {
        this.padding = padding;
        return this;
    }

    public Widget addHint(long hint) {
        hints |= hint;
        return this;
    }

    public boolean hasHint(long hint) {
        return (hints & hint) != 0;
    }

    public Widget setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    public <T> T getProperty(String name) {
        return (T) properties.get(name);
    }


    public <T> T getWidgetByPath(String... names) {

        Widget root = this;


        for(String name : names) {
            for(Widget widget : root.getWidgets()) {
                if(widget.getName() != null && widget.getName().equals(name)) {
                    root = widget;
                }
            }

        }

        return (T) root;
    }

    public abstract int getRequiredWidth();
    public abstract int getRequiredHeight();

    public abstract void update(UIPainter platform, int x, int y, int w, int h);
    public void updateChildren(UIPainter platform, int x, int y, int w, int h) {
        layoutEngine.updateChildren(platform, x, y, w, h);
    }
    public Widget setLayoutEngine(LayoutEngine layoutEngine) {
        this.layoutEngine = layoutEngine;
        layoutEngine.setWidget(this);
        return this;
    }
    public Widget addEventHandler(EventHandler eventHandler) {
        eventHandlers.add(eventHandler);
        return this;
    }

    public List<EventHandler> getEventHandlers() {
        return eventHandlers;
    }

    public void removeEventHandler(EventHandler eventHandler) {
        eventHandlers.remove(eventHandler);
    }

    public Widget addWidget(Widget widget) {
        widgets.add(widget);
        widget.parent = this;
        widget.onAdded();
        return this;
    }

    public void onAdded() {}

    public List<Widget> getWidgets() {
        return widgets;
    }

    public Widget addWidgets(Widget... widgets) {
        for(Widget widget : widgets) addWidget(widget);
        return this;
    }


}
