package engine.gameui;

import engine.input.Input;
import engine.input.SurfaceCharCallback;
import engine.input.SurfaceKeyCallback;
import engine.Time;
import engine.graphics.Color;
import engine.graphics.Rect2D;
import engine.graphics.Session;
import engine.graphics.text.MsdfFont;
import org.joml.Vector2f;

;

public class TextField extends Widget {
    private TextValue value;
    private MsdfFont font;
    private SurfaceCharCallback surfaceCharCallback;
    private SurfaceKeyCallback surfaceKeyCallback;
    private int columns = 15;
    private int cursor = 0;
    private int visibilityCursor = 0;
    private float cursorBlinkTime = 0.3f;
    private boolean cursorVisible = true;
    private float time = 0;
    private float w = 0;

    public TextField(TextValue value, MsdfFont font) {
        this.value = value;
        this.font = font;
        w = font.getStringWidth(value.string.toString());
        surfaceKeyCallback = (key, modifiers) -> {
            if(focused) {
                if (key == Input.KEY_BACKSPACE) {
                    if (value.string.isEmpty() || cursor == 0) return;
                    value.string.deleteCharAt(cursor - 1);
                    cursor--;
                }

                if (key == Input.KEY_LEFT) if (cursor > 0) cursor--;
                if (key == Input.KEY_RIGHT) if (cursor < value.string.length()) cursor++;


                if(key == Input.KEY_V && (modifiers & Input.MOD_CONTROL) != 0) {
                    value.string.append(Session.getSurface().getClipboardString());
                }
            }
        };
        surfaceCharCallback = new SurfaceCharCallback() {
            @Override
            public void keyClick(char c) {
                if(focused) {
                    value.string.insert(cursor, c);
                    cursor++;
                }
            }
        };

        columns = value.string.length();
        cursor += columns / 2;



        Session.getSurface().addKeyCallback(surfaceKeyCallback);
        Session.getSurface().addCharCallback(surfaceCharCallback);
    }

    public int getColumns() {
        return columns;
    }

    public TextField setColumns(int columns) {
        this.columns = columns;
        return this;
    }


    private String getVisibleString() {
        if(cursor - value.string.length() > columns)
            visibilityCursor = Math.clamp(cursor - columns / 2, 0, value.string.length() - 1);
        return value.string.substring(visibilityCursor, Math.min(visibilityCursor + columns, value.string.length()));
    }

    @Override
    public int getRequiredWidth() {
        return (int) (w + (padding * 2));
    }

    public TextValue getText() {
        return value;
    }

    @Override
    public int getRequiredHeight() {
        return (int) font.getStringHeight(value.string.toString()) + (2 * padding);
    }

    @Override
    public void update(UIPainter platform, int x, int y, int w, int h) {
        String visibleString = getVisibleString();
        Vector2f mousePos = Session.getSurface().getMousePos();
        if(Session.getSurface().getMousePressed(Input.MOUSE_BUTTON_1)) {
            focused = Rect2D.contains(mousePos.x, mousePos.y, x + padding, y + padding, w - padding * 2, h - padding * 2);

            if(focused) {
                float sx = mousePos.x - x - padding;

                for(int i = 0; i < getText().string.length(); i++) {
                    float width = font.getStringWidth(getText().string.substring(0, i));
                    float width2 = font.getStringWidth(getText().string.substring(0, Math.min(i + 1, getText().string.length() - 1)));
                    if(width <= sx && sx <= width2) {
                        cursor = i;
                        cursorVisible = true;
                        break;
                    }
                }


            }

        }

        platform.drawString(
                x + padding,
                y + padding,
                visibleString,
                font,
                null,
                platform.getTheme().textColor
        );

        Color color = focused ? platform.getTheme().buttonHoverColor : platform.getTheme().containerBackgroundColor;

        time += Time.deltaTime();
        if(time >= cursorBlinkTime) {
            cursorVisible = !cursorVisible;
            time -= cursorBlinkTime;
        }

        if(focused) {
            if (cursorVisible) {
                int end = 0;
                if(!visibleString.isEmpty()) end = cursor % visibleString.length();

                float cx = font.getStringWidth(visibleString.substring(0, end));
                platform.drawRect(x + padding + cx, y + padding * 2, 1, h - padding * 4, platform.getTheme().textColor);
            }
        }


        platform.drawRectLines(x + padding, y + padding, w - padding * 2, h - padding * 2, 2, color);
        updateChildren(platform, x + padding, y + padding, w - padding * 2, h - padding * 2);
    }
}
