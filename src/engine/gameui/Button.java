package engine.gameui;

import engine.graphics.Session;
import engine.input.Input;
import engine.graphics.Rect2D;
import engine.graphics.text.MsdfFont;
import org.joml.Vector2f;

public class Button extends Widget {
    private TextValue value;
    private MsdfFont font;
    private boolean pressed;

    public Button(TextValue value, MsdfFont font) {
        this.value = value;
        this.font = font;
    }

    @Override
    public int getRequiredWidth() {
        return (int) font.getStringWidth(value.string.toString()) + (4 * padding);
    }

    @Override
    public int getRequiredHeight() {
        return (int) font.getStringHeight(value.string.toString()) + (4 * padding);
    }

    @Override
    public void update(UIPainter platform, int x, int y, int w, int h) {
        Vector2f mousePos = Session.getSurface().getMousePos();

        if(Rect2D.contains(mousePos.x, mousePos.y, x + padding, y + padding, w - padding * 2, h - padding * 2)){
            platform.drawRect(x + padding, y + padding, w - padding * 2, h - padding * 2, platform.getTheme().buttonHoverColor);

            boolean inputPressed = Session.getSurface().getMousePressed(Input.MOUSE_BUTTON_1);
            if(pressed) {
                platform.drawRect(x + padding, y + padding, w - padding * 2, h - padding * 2, platform.getTheme().buttonClickColor);

            }
            else {
                platform.drawRect(x + padding, y + padding, w - padding * 2, h - padding * 2, platform.getTheme().buttonHoverColor);
            }


            if(pressed != inputPressed){

                if(pressed)
                    for(EventHandler eventHandler : getEventHandlers()) eventHandler.onClick();

                pressed = inputPressed;
            }

        }
        else
            platform.drawRect(x + padding, y + padding, w - padding * 2, h - padding * 2, platform.getTheme().buttonBackgroundColor);

        platform.drawString(x + (2 * padding), y + (2 * padding), value.string.toString(), font, null, platform.getTheme().textColor);
        updateChildren(platform, x + padding, y + padding, w - padding * 2, h - padding * 2);
    }
}
