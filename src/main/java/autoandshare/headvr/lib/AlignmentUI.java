package autoandshare.headvr.lib;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.opengl.Matrix;
import android.text.TextPaint;

import com.google.vr.sdk.base.Eye;

import java.util.Locale;

import autoandshare.headvr.lib.rendering.VRSurface;

public class AlignmentUI extends VRSurface {
    public static final int PATTERN_ALIGN = 0;
    public static final int PATTERN_FOCUS = 1;
    public static final int PATTERN_TEXT = 2;
    public static final int PATTERN_COLOR = 3;
    private static final int PATTERN_COUNT = 4;
    private static final String[] PATTERN_NAMES = {"ALIGN", "FOCUS", "TEXT", "COLOR"};

    private static final int SIZE_PIXEL = 1024;
    private static final float PIXELS_PER_METER = SIZE_PIXEL / 3f;
    private static final float PANEL_SIZE = SIZE_PIXEL / PIXELS_PER_METER;
    private static final float PANEL_DISTANCE = 3f;
    private static final int CENTER = SIZE_PIXEL / 2;

    private volatile int pattern = PATTERN_ALIGN;
    private volatile boolean dirty = true;
    private int drawnPattern = -1;
    private int drawnEyeDistance = Integer.MIN_VALUE;
    private boolean drawnAnchored = false;

    private final Paint dimPaint;
    private final Paint grayPaint;
    private final Paint whiteLinePaint;
    private final Paint thickPaint;
    private final Paint dotPaint;
    private final Paint fillPaint;
    private final TextPaint textLeft;
    private final TextPaint textRight;
    private final TextPaint textCenter;
    private final TextPaint textSmallGray;
    private final TextPaint textBig;
    private final TextPaint text44;
    private final TextPaint text36;
    private final TextPaint text28;
    private final TextPaint text22;
    private final TextPaint text18;
    private final TextPaint text16;

    public AlignmentUI() {
        init(PANEL_SIZE, PANEL_SIZE, PANEL_DISTANCE,
                new PointF(-PANEL_SIZE / 2, PANEL_SIZE / 2), SIZE_PIXEL, SIZE_PIXEL);

        // Match 2D video behaviour: vertical offset applies and the eye distance
        // setting shifts the panel between both eyes (near <-> far).
        verticalFixed = false;
        worldAnchored = false;
        worldRecenter = new float[16];

        dimPaint = strokePaint(Color.DKGRAY, 1);
        grayPaint = strokePaint(Color.GRAY, 1);
        whiteLinePaint = strokePaint(Color.WHITE, 2);
        thickPaint = strokePaint(Color.WHITE, 4);
        dotPaint = solidPaint(Color.WHITE);
        fillPaint = solidPaint(Color.WHITE);

        textLeft = textPaint(Color.LTGRAY, 24, Paint.Align.LEFT);
        textRight = textPaint(Color.LTGRAY, 24, Paint.Align.RIGHT);
        textCenter = textPaint(Color.LTGRAY, 22, Paint.Align.CENTER);
        textSmallGray = textPaint(Color.GRAY, 18, Paint.Align.CENTER);
        textBig = textPaint(Color.WHITE, 56, Paint.Align.CENTER);
        text44 = textPaint(Color.WHITE, 44, Paint.Align.CENTER);
        text36 = textPaint(Color.WHITE, 36, Paint.Align.CENTER);
        text28 = textPaint(Color.WHITE, 28, Paint.Align.CENTER);
        text22 = textPaint(Color.WHITE, 22, Paint.Align.CENTER);
        text18 = textPaint(Color.WHITE, 18, Paint.Align.CENTER);
        text16 = textPaint(Color.WHITE, 16, Paint.Align.CENTER);
    }

    private static Paint strokePaint(int color, int width) {
        Paint p = new Paint();
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        return p;
    }

    private static Paint solidPaint(int color) {
        Paint p = new Paint();
        p.setColor(color);
        return p;
    }

    private static TextPaint textPaint(int color, float size, Paint.Align align) {
        TextPaint p = new TextPaint();
        p.setColor(color);
        p.setTextSize(size);
        p.setTextAlign(align);
        return p;
    }

    public void nextPattern() {
        pattern = (pattern + 1) % PATTERN_COUNT;
        dirty = true;
    }

    public void prevPattern() {
        pattern = (pattern + PATTERN_COUNT - 1) % PATTERN_COUNT;
        dirty = true;
    }

    public void toggleAnchor() {
        worldAnchored = !worldAnchored;
        dirty = true;
    }

    public void invalidate() {
        dirty = true;
    }

    public void captureHome(float[] headView) {
        Matrix.transposeM(worldRecenter, 0, headView, 0);
        worldRecenter[3] = 0f;
        worldRecenter[7] = 0f;
        worldRecenter[11] = 0f;
    }

    public void glDraw(Eye eye) {
        if (eye.getType() == 1) {
            int value = Setting.Instance.get(Setting.id.EyeDistance);
            if (dirty || pattern != drawnPattern
                    || value != drawnEyeDistance
                    || worldAnchored != drawnAnchored) {
                Canvas canvas = getCanvas();
                drawPattern(canvas);
                releaseCanvas(canvas);
                drawnPattern = pattern;
                drawnEyeDistance = value;
                drawnAnchored = worldAnchored;
                dirty = false;
            }
        }
        this.draw(eye);
    }

    private void drawPattern(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        switch (pattern) {
            case PATTERN_ALIGN:
                drawAlign(canvas);
                break;
            case PATTERN_FOCUS:
                drawFocus(canvas);
                break;
            case PATTERN_TEXT:
                drawTextPattern(canvas);
                break;
            default:
                drawColorPattern(canvas);
                break;
        }
        drawFooter(canvas);
    }

    private void drawString(Canvas canvas, String s, float x, float y, Paint paint) {
        canvas.drawText(s, x,
                y - (paint.descent() + paint.ascent()) / 2,
                paint);
    }

    private void drawAlign(Canvas canvas) {
        // diagonals
        canvas.drawLine(0, 0, SIZE_PIXEL, SIZE_PIXEL, dimPaint);
        canvas.drawLine(SIZE_PIXEL, 0, 0, SIZE_PIXEL, dimPaint);

        // centre grid
        for (int i = -192; i <= 192; i += 32) {
            canvas.drawLine(CENTER + i, CENTER - 192, CENTER + i, CENTER + 192, dimPaint);
            canvas.drawLine(CENTER - 192, CENTER + i, CENTER + 192, CENTER + i, dimPaint);
        }

        // circles
        for (int r = 64; r <= 448; r += 64) {
            canvas.drawCircle(CENTER, CENTER, r, grayPaint);
        }

        // cross
        canvas.drawLine(0, CENTER, SIZE_PIXEL, CENTER, whiteLinePaint);
        canvas.drawLine(CENTER, 0, CENTER, SIZE_PIXEL, whiteLinePaint);

        // cross ticks
        for (int i = 32; i < CENTER; i += 32) {
            int len = (i % 128 == 0) ? 16 : 8;
            canvas.drawLine(CENTER + i, CENTER - len, CENTER + i, CENTER + len, whiteLinePaint);
            canvas.drawLine(CENTER - i, CENTER - len, CENTER - i, CENTER + len, whiteLinePaint);
            canvas.drawLine(CENTER - len, CENTER + i, CENTER + len, CENTER + i, whiteLinePaint);
            canvas.drawLine(CENTER - len, CENTER - i, CENTER + len, CENTER - i, whiteLinePaint);
        }

        // edge ruler ticks
        for (int i = 32; i < SIZE_PIXEL; i += 32) {
            int len = (i % 128 == 0) ? 24 : 10;
            canvas.drawLine(i, 0, i, len, grayPaint);
            canvas.drawLine(i, SIZE_PIXEL - len, i, SIZE_PIXEL, grayPaint);
            canvas.drawLine(0, i, len, i, grayPaint);
            canvas.drawLine(SIZE_PIXEL - len, i, SIZE_PIXEL, i, grayPaint);
        }

        // corner brackets
        int inset = 8;
        int arm = 48;
        canvas.drawLine(inset, inset, inset + arm, inset, thickPaint);
        canvas.drawLine(inset, inset, inset, inset + arm, thickPaint);
        canvas.drawLine(SIZE_PIXEL - inset, inset, SIZE_PIXEL - inset - arm, inset, thickPaint);
        canvas.drawLine(SIZE_PIXEL - inset, inset, SIZE_PIXEL - inset, inset + arm, thickPaint);
        canvas.drawLine(inset, SIZE_PIXEL - inset, inset + arm, SIZE_PIXEL - inset, thickPaint);
        canvas.drawLine(inset, SIZE_PIXEL - inset, inset, SIZE_PIXEL - inset - arm, thickPaint);
        canvas.drawLine(SIZE_PIXEL - inset, SIZE_PIXEL - inset, SIZE_PIXEL - inset - arm,
                SIZE_PIXEL - inset, thickPaint);
        canvas.drawLine(SIZE_PIXEL - inset, SIZE_PIXEL - inset, SIZE_PIXEL - inset,
                SIZE_PIXEL - inset - arm, thickPaint);

        // centre target
        canvas.drawCircle(CENTER, CENTER, 32, whiteLinePaint);
        canvas.drawCircle(CENTER, CENTER, 7, dotPaint);

        drawString(canvas,
                "adjust the phone in the viewer so the crosshair sits centred and is sharp in each eye",
                CENTER, 952, textSmallGray);
    }

    private void drawFocus(Canvas canvas) {
        int rMin = 84;
        int rMax = 300;
        for (int i = 0; i < 36; i++) {
            double a = Math.toRadians(i * 10);
            canvas.drawLine(
                    CENTER + (float) (rMin * Math.cos(a)), CENTER + (float) (rMin * Math.sin(a)),
                    CENTER + (float) (rMax * Math.cos(a)), CENTER + (float) (rMax * Math.sin(a)),
                    whiteLinePaint);
        }
        canvas.drawCircle(CENTER, CENTER, rMin, grayPaint);
        canvas.drawCircle(CENTER, CENTER, rMax, whiteLinePaint);
        canvas.drawCircle(CENTER, CENTER, 8, dotPaint);

        int[] pitch = {8, 4, 2};
        int[] zone = {176, 416, 656};
        for (int z = 0; z < 3; z++) {
            for (int x = zone[z]; x < zone[z] + 192; x += pitch[z] * 2) {
                canvas.drawRect(x, 96, x + pitch[z], 160, dotPaint);
                canvas.drawRect(x, SIZE_PIXEL - 160, x + pitch[z], SIZE_PIXEL - 96, dotPaint);
            }
            for (int y = zone[z]; y < zone[z] + 192; y += pitch[z] * 2) {
                canvas.drawRect(96, y, 160, y + pitch[z], dotPaint);
                canvas.drawRect(SIZE_PIXEL - 160, y, SIZE_PIXEL - 96, y + pitch[z], dotPaint);
            }
        }

        drawString(canvas,
                "star and bar patterns: the 2 px bars need the sharpest focus (check each eye, try moving the phone closer/farther inside the viewer)",
                CENTER, 952, textSmallGray);
    }

    private void drawTextPattern(Canvas canvas) {
        canvas.drawLine(0, CENTER, SIZE_PIXEL, CENTER, dimPaint);
        canvas.drawLine(CENTER, 0, CENTER, SIZE_PIXEL, dimPaint);

        int y = 158;
        drawString(canvas, "Sphinx of black quartz", CENTER, y, textBig);
        y += 108;
        drawString(canvas, "judge my vow. Pack my box", CENTER, y, text44);
        y += 84;
        drawString(canvas, "with five dozen liquor jugs.", CENTER, y, text36);
        y += 66;
        drawString(canvas, "Waltz, bad nymph, for quick jigs vex!", CENTER, y, text28);
        y += 54;
        drawString(canvas, "How vexingly quick daft zebras jump!", CENTER, y, text22);
        y += 48;
        drawString(canvas, "0123456789 ABCDEFGHIJKLM NOPQRSTUVWXYZ", CENTER, y, text18);
        y += 40;
        drawString(canvas, "the quick brown fox jumps over the lazy dog", CENTER, y, text16);
        y += 36;
        drawString(canvas, "Il1| iIl1| O0o (){}[] 88 77 66 99", CENTER, y, text16);

        drawString(canvas,
                "small text should stay readable; blurry, doubled or ghosted glyphs mean the focus is off",
                CENTER, 952, textSmallGray);
    }

    private void drawColorPattern(Canvas canvas) {
        fillPaint.setColor(Color.RED);
        canvas.drawRect(32, 32, 416, 416, fillPaint);
        fillPaint.setColor(Color.GREEN);
        canvas.drawRect(608, 32, 992, 416, fillPaint);
        fillPaint.setColor(Color.BLUE);
        canvas.drawRect(32, 608, 416, 992, fillPaint);
        fillPaint.setColor(Color.WHITE);
        canvas.drawRect(608, 608, 992, 992, fillPaint);

        // test area frame
        canvas.drawRect(324, 324, 700, 656, grayPaint);

        // red/blue vertical bars (lateral colour error)
        for (int x = 336; x < 688; x += 8) {
            fillPaint.setColor(Color.RED);
            canvas.drawRect(x, 336, x + 4, 424, fillPaint);
            fillPaint.setColor(Color.BLUE);
            canvas.drawRect(x + 4, 336, x + 8, 424, fillPaint);
        }

        // grey ramp
        for (int i = 0; i < 17; i++) {
            int v = i * 255 / 16;
            fillPaint.setColor(Color.rgb(v, v, v));
            float x0 = 336 + i * (352f / 17f);
            canvas.drawRect(x0, 440, x0 + (352f / 17f) + 1, 500, fillPaint);
        }

        // checkerboard
        fillPaint.setColor(Color.WHITE);
        for (int y0 = 516; y0 < 644; y0 += 16) {
            for (int x0 = 336; x0 < 464; x0 += 16) {
                if ((((x0 - 336) / 16) + ((y0 - 516) / 16)) % 2 == 0) {
                    canvas.drawRect(x0, y0, x0 + 16, y0 + 16, fillPaint);
                }
            }
        }

        // fine white/black stripes
        for (int x = 480; x < 688; x += 8) {
            canvas.drawRect(x, 516, x + 4, 644, fillPaint);
        }

        drawString(canvas,
                "colour fields and fine stripes: edges should not split into red/blue or ghost",
                CENTER, 952, textSmallGray);
    }

    private void drawFooter(Canvas canvas) {
        int value = Setting.Instance.get(Setting.id.EyeDistance);
        float valueF = Setting.Instance.getFloatValue(Setting.id.EyeDistance, value);

        drawString(canvas, String.format(Locale.US, "ALIGNMENT & FOCUS TEST  -  pattern %d/%d %s",
                pattern + 1, PATTERN_COUNT, PATTERN_NAMES[pattern]), 16, 30, textLeft);

        drawString(canvas, String.format(Locale.US, "%s   eye distance %d (%+.2f)  volume keys",
                worldAnchored ? "world locked" : "head locked", value, valueF),
                SIZE_PIXEL - 16, 30, textRight);

        drawString(canvas, "tap: next pattern   double tap: recentre / world lock   back: exit",
                CENTER, SIZE_PIXEL - 30, textCenter);
    }
}
