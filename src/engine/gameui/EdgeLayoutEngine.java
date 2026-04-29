package engine.gameui;

public class EdgeLayoutEngine extends LayoutEngine {
    public static long Top = 1;
    public static long Bottom = 1 << 1;
    public static long Left = 1 << 2;
    public static long Right = 1 << 3;
    public static long Center = 1 << 4;

    @Override
    public int getComputedWidth() {

        int top = 0, bottom = 0, center = 0;

        for(Widget child : widget.getWidgets()) {
            if (child.hasHint(Top)) top = child.getRequiredWidth();
            if(child.hasHint(Center)) center += child.getRequiredWidth();
            else if (child.hasHint(Bottom)) bottom = child.getRequiredWidth();
        }

        int width = Math.max(center, Math.max(top, bottom));
        if(width != 0) return width;

        for(Widget child : widget.getWidgets()) {
            if(child.hasHint(Left)) width += child.getRequiredWidth();
            if(child.hasHint(Center)) width += child.getRequiredWidth();
            if(child.hasHint(Right)) width += child.getRequiredWidth();
        }

        return width;

    }

    @Override
    public int getComputedHeight() {
        int height = 0;
        int leftHeight = 0, rightHeight = 0, centerHeight = 0;

        for(Widget child : widget.getWidgets()) {
            if(child.hasHint(Top)) height += child.getRequiredHeight();
            if(child.hasHint(Bottom)) height += child.getRequiredHeight();
            if(child.hasHint(Left)) leftHeight = child.getRequiredHeight();
            if(child.hasHint(Center)) centerHeight = child.getRequiredHeight();
            if(child.hasHint(Right)) rightHeight = child.getRequiredHeight();
        }

        height += Math.max(centerHeight, Math.max(leftHeight, rightHeight));

        return height;
    }

    @Override
    public void updateChildren(GfxPlatform platform, int x, int y, int w, int h) {

        int dx = x, dy = y;
        Widget myWidget = getWidget();
        int widgetWidth = w - myWidget.padding, widgetHeight = h - myWidget.padding;

        int topOffset = 0, bottomOffset = 0;

        for(Widget child : widget.getWidgets()) {
            int childWidth = child.getRequiredWidth();
            int childHeight = child.getRequiredHeight();

            if(child.hasHint(Top)) {
                child.update(platform, dx, dy, widgetWidth, childHeight);
                topOffset = childHeight;
            }
            else if(child.hasHint(Bottom)) {
                child.update(platform, dx, dy + (widgetHeight - childHeight), widgetWidth, childHeight);
                bottomOffset = childHeight;
            }
            else if(child.hasHint(Right)) {
                 child.update(platform, dx + (widgetWidth - childWidth), dy + topOffset, childWidth, widgetHeight - topOffset - bottomOffset);
            }
            else if(child.hasHint(Left)) {
                child.update(platform, dx, dy + topOffset, child.getRequiredWidth(), widgetHeight - topOffset - bottomOffset);
            }
            else if(child.hasHint(Center)) {

                float hw = childWidth / 2f;
                float hh = childHeight / 2f;

                child.update(platform, (int) (dx + (widgetWidth / 2f - hw)), (int) (dy + (widgetHeight / 2f - hh)), childWidth, childHeight);
            }
            else {
                child.update(platform, dx, dy, childWidth, childHeight);
            }




        }


    }
}
