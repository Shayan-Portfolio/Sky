package engine.gameui;

import engine.Time;

public class FunctionGraph extends Widget {
    private int width, height;
    private Function function;
    private float scale;

    public FunctionGraph(int width, int height, float scale, Function function) {
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.function = function;
    }

    @Override
    public int getRequiredWidth() {
        return width;
    }

    @Override
    public int getRequiredHeight() {
        return height;
    }

    private float phase = 0;
    @Override
    public void update(Paint paint, int x, int y, int w, int h) {
        paint.drawRect(x, y, w, h, paint.getTheme().containerBackgroundColor);

        int steps = 30;

        float ws = (float) w / steps;
        float hs = (float) h / steps;

        float lx = 0, ly = 0;

        phase += Time.deltaTime * 10f;



        for(int i = 0; i < steps; i++) {
            if(i % 4 == 0) {
                paint.drawRect(x + (i * ws), y, 1, h, paint.getTheme().containerBackgroundColor);
                paint.drawRect(x, y + (i * hs), w, 1, paint.getTheme().containerBackgroundColor);
            }
        }

        for(int i = 0; i < steps; i++) {
            float nx = i * ws;
            float fofx = (function.f(nx + phase) / scale) * (h / 2f);



            paint.drawLine(x + lx, y + ly, x + nx, y + fofx + hs * (steps / 2f), paint.getTheme().buttonHoverColor);


            lx = nx;
            ly = fofx + hs * (steps / 2f);
        }




    }
}
