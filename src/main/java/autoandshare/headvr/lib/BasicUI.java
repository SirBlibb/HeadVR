package autoandshare.headvr.lib;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.opengl.Matrix;
import android.text.TextPaint;
import android.text.TextUtils;

import com.google.vr.sdk.base.Eye;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import autoandshare.headvr.lib.controller.headcontrol.HeadControl;
import autoandshare.headvr.lib.rendering.VRSurface;

public class BasicUI extends VRSurface {
    public static final int TARGET_NONE = -1;
    public static final int TARGET_PREV = 0;
    public static final int TARGET_REWIND = 1;
    public static final int TARGET_PLAY = 2;
    public static final int TARGET_FORWARD = 3;
    public static final int TARGET_NEXT = 4;
    public static final int TARGET_OPTIONS = 5;
    public static final int TARGET_PROGRESS = 6;
    public static final int TARGET_RECENTER = 7;

    private static final int DWELL_MS = 2000;
    private static final long SHOW_UI_DELAY_NS = 150000000L;
    private static final float PIXELS_PER_METER = 360f;
    private static final float BUTTON_TOP = 44f;
    private static final float BUTTON_BOTTOM = 94f;
    private static final float BUTTON_HALF_WIDTH = 26f;
    private static final float PROGRESS_TOP = 100f;
    private static final float PROGRESS_BOTTOM = 132f;
    private static final int[] BUTTON_CENTERS = {28, 80, 132, 184, 236, 288, 340};
    private static final int[] BUTTON_TARGETS = {
            TARGET_PREV, TARGET_REWIND, TARGET_PLAY, TARGET_FORWARD, TARGET_NEXT, TARGET_OPTIONS,
            TARGET_RECENTER};
    private static final float[] GAZE_ORIGIN = {0f, 0f, 0f, 1f};
    private static final float[] GAZE_FORWARD = {0f, 0f, -1f, 0f};

    private State state;

    private float panelLeft;
    private float panelTop;
    private float panelWidth;
    private float panelHeight;
    private final float panelDistance = 3f;

    private boolean gazeCaptured = false;
    private boolean gazeOnPanel = false;
    private long gazeOnPanelSinceNs = 0;
    private float gazeCanvasX = 0f;
    private float gazeCanvasY = 0f;
    private int gazeTarget = TARGET_NONE;
    private int gazeFiredTarget = TARGET_NONE;
    private float gazeDwell = 0f;
    private long gazeDwellStartNs = 0;
    private final float[] gazeView = new float[16];
    private final float[] gazeInverse = new float[16];
    private final float[] gazeOrigin = new float[4];
    private final float[] gazeDirection = new float[4];

    private TextPaint leftAlignTextPaint;
    private Paint rightAlignTextPaint;
    private Paint centerAlignTextPaint;
    private Paint progressLinePaint1;
    private Paint progressLinePaint2;
    private Paint progressLinePaint3;

    // UI has 3 rows
    private float row0Y;
    private float row1Y;
    private float row2Y;
    private float beginX;
    private float endX;
    private float motionsX;

    public BasicUI(State state) {
        this.state = state;

        float widthPixel = 920;
        float heightPixel = 138;

        initSurface(widthPixel, heightPixel);

        float textSize = 30;
        float strokeWidth = 16;
        float margin = 10;


        row0Y = heightPixel / 6;
        row1Y = heightPixel * 3 / 6;
        row2Y = heightPixel * 5 / 6;

        beginX = margin;
        endX = widthPixel - margin;
        motionsX = margin + (endX - beginX) * 0.4f;

        leftAlignTextPaint = new TextPaint();
        leftAlignTextPaint.setColor(Color.LTGRAY);
        leftAlignTextPaint.setTextSize(textSize);
        leftAlignTextPaint.setTextAlign(Paint.Align.LEFT);

        rightAlignTextPaint = new Paint();
        rightAlignTextPaint.setColor(Color.LTGRAY);
        rightAlignTextPaint.setTextSize(textSize);
        rightAlignTextPaint.setTextAlign(Paint.Align.RIGHT);

        centerAlignTextPaint = new Paint();
        centerAlignTextPaint.setColor(Color.LTGRAY);
        centerAlignTextPaint.setTextSize(textSize);
        centerAlignTextPaint.setTextAlign(Paint.Align.CENTER);

        progressLinePaint1 = new Paint();
        progressLinePaint1.setColor(Color.LTGRAY);
        progressLinePaint1.setStrokeWidth(strokeWidth);

        progressLinePaint2 = new Paint();
        progressLinePaint2.setColor(Color.DKGRAY);
        progressLinePaint2.setStrokeWidth(strokeWidth);

        progressLinePaint3 = new Paint();
        progressLinePaint3.setColor(Color.GREEN);
        progressLinePaint3.setStrokeWidth(strokeWidth / 2);

        hoverBgPaint = new Paint();
        hoverBgPaint.setColor(0x55FFFFFF);

        dwellRingPaint = new Paint();
        dwellRingPaint.setStyle(Paint.Style.STROKE);
        dwellRingPaint.setStrokeWidth(3);
        dwellRingPaint.setColor(Color.LTGRAY);

        dwellProgressPaint = new Paint();
        dwellProgressPaint.setStyle(Paint.Style.STROKE);
        dwellProgressPaint.setStrokeWidth(5);
        dwellProgressPaint.setColor(Color.GREEN);

        cursorPaint = new Paint();
        cursorPaint.setColor(Color.WHITE);

        cursorOutlinePaint = new Paint();
        cursorOutlinePaint.setColor(Color.BLACK);

        activeTextPaint = new TextPaint();
        activeTextPaint.setColor(Color.GREEN);
        activeTextPaint.setTextSize(textSize);
        activeTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    private Paint hoverBgPaint;
    private Paint dwellRingPaint;
    private Paint dwellProgressPaint;
    private Paint cursorPaint;
    private Paint cursorOutlinePaint;
    private TextPaint activeTextPaint;

    private void initSurface(float widthPixel, float heightPixel) {
        float uiWidth = widthPixel / PIXELS_PER_METER;
        float uiHeight = heightPixel / PIXELS_PER_METER;
        panelLeft = -uiWidth / 2;
        panelTop = -1.55f;
        panelWidth = uiWidth;
        panelHeight = uiHeight;
        init(uiWidth, uiHeight, panelDistance,
                new PointF(panelLeft, panelTop),
                (int) widthPixel, (int) heightPixel);
        worldAnchored = true;
        worldRecenter = new float[16];
    }

    public void glDraw(Eye eye) {
        if (eye.getType() == 1) {
            Canvas canvas = this.getCanvas();

            applyFade();
            canvas.drawColor(Color.argb((int) (255 * fade), 0, 0, 0));

            drawFileNameOrMessage(canvas);

            drawDatetime(canvas);

            drawMotions(canvas);

            if (state.errorMessage != null) {
                drawString(canvas, "Error", beginX, row1Y, leftAlignTextPaint);
                drawString(canvas, state.errorMessage, beginX, row2Y, leftAlignTextPaint);
            } else if (state.videoLoaded) {
                drawButtons(canvas);
                drawBuffering(canvas);
                drawTagAndTime(canvas);
                drawProgress(canvas);
                drawGaze(canvas);
            } else {
                drawString(canvas,
                        (state.playerState != null) ? state.playerState : "Loading"
                                + (state.playing ? "" : " (Paused) "),
                        beginX, row1Y, leftAlignTextPaint);
            }

            this.releaseCanvas(canvas);
        }
        this.draw(eye);

    }

    private String getFileNameOrTitle() {
        return state.isFileProtocol ?
                state.fileName : state.title;
    }

    private void drawFileNameOrMessage(Canvas canvas) {
        String fullTxt = (state.message != null) ?
                state.message :
                "" + state.indexString + " " + getFileNameOrTitle();

        drawString(canvas,
                TextUtils.ellipsize(fullTxt, leftAlignTextPaint, (endX - beginX) * 3 / 4, TextUtils.TruncateAt.END).toString(),
                beginX, row0Y, leftAlignTextPaint);
    }

    private SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa", Locale.US);

    private float drawDatetime(Canvas canvas) {
        String string = sdf.format(Calendar.getInstance().getTime());
        if (((System.currentTimeMillis() / 1000) % 2) == 0) {
            string = string.replace(':', ' ');
        }
        drawString(canvas, string, endX, row0Y, rightAlignTextPaint);
        return rightAlignTextPaint.measureText(string);
    }


    private void drawButtons(Canvas canvas) {
        for (int i = 0; i < BUTTON_CENTERS.length; i++) {
            int target = BUTTON_TARGETS[i];
            float centerX = BUTTON_CENTERS[i];

            if (gazeTarget == target) {
                canvas.drawRoundRect(centerX - 26, BUTTON_TOP,
                        centerX + 26, BUTTON_BOTTOM, 8, 8, hoverBgPaint);
            }

            String glyph;
            Paint paint = centerAlignTextPaint;
            switch (target) {
                case TARGET_PREV:
                    glyph = "\u23EE";
                    break;
                case TARGET_REWIND:
                    glyph = "\u23EA";
                    if (state.seeking && !state.forward) {
                        paint = activeTextPaint;
                    }
                    break;
                case TARGET_PLAY:
                    glyph = state.playing ? "\u23F8" : "\u25B6";
                    break;
                case TARGET_FORWARD:
                    glyph = "\u23E9";
                    if (state.seeking && state.forward) {
                        paint = activeTextPaint;
                    }
                    break;
                case TARGET_NEXT:
                    glyph = "\u23ED";
                    break;
                case TARGET_RECENTER:
                    glyph = "\u21BB";
                    break;
                default:
                    glyph = "\u2699";
                    break;
            }
            drawString(canvas, glyph, centerX, row1Y, paint);
        }
    }

    private void drawString(Canvas canvas, String s, float x, float y, Paint paint) {

        canvas.drawText(s, x,
                y - (paint.descent() + paint.ascent()) / 2,
                paint);
    }

    private void drawMotions(Canvas canvas) {
        String string = "";
        if (state.motions != null) {
            string = state.motions;
        }
        if (HeadControl.HeadControlLocked) {
            string = "\uD83D\uDD12" + " " + string;
        }
        drawString(canvas, string, motionsX, row1Y, leftAlignTextPaint);
    }

    private String modeTag() {
        if (state.videoType == null) {
            return "";
        }

        String vr = "";
        if (state.videoType.type == VideoType.Type.VR180) {
            vr = "180";
        } else if (state.videoType.type == VideoType.Type.VR360) {
            vr = "360";
        } else if (state.videoType.type == VideoType.Type.Fisheye180) {
            vr = "F180";
        } else if (state.videoType.type == VideoType.Type.Fisheye180Equisolid) {
            vr = "F180e";
        }

        if (state.is2DContent()) {
            return vr.length() > 0 ? vr : "2D";
        }

        StringBuilder tag = new StringBuilder();
        if (state.videoType.isHalf()) {
            tag.append("H");
        } else if (state.videoType.isFull()) {
            tag.append("F");
        }
        if (state.videoType.isSBS()) {
            tag.append("SBS");
        } else if (state.videoType.isTAB()) {
            tag.append("TAB");
        }
        if (vr.length() > 0) {
            tag.append(" ").append(vr);
        }
        return tag.toString();
    }

    private String seekingTime() {
        return state.seeking && (state.videoLength != 0) ?
                "  < " + formatTime((int)(state.videoLength * state.newPosition)) + " >  " : "";
    }

    private void drawBuffering(Canvas canvas) {
        if (state.playerState != null && state.playerState.startsWith("Buffering")) {
            drawString(canvas, state.playerState, motionsX, row1Y, leftAlignTextPaint);
        }
    }

    private void drawTagAndTime(Canvas canvas) {
        String tag = modeTag();
        String time;
        if (state.videoLength > 0) {
            time = formatTime(state.currentTime) + " / " + formatTime(state.videoLength);
        } else {
            time = "Live   " + formatTime(state.currentTime);
        }
        drawString(canvas, (tag.length() > 0 ? tag + "   " : "") +
                        seekingTime() + time,
                endX, row1Y, rightAlignTextPaint);
    }

    private void drawProgress(Canvas canvas) {
        float middle = beginX + (endX - beginX) * state.currentPosition;

        canvas.drawLine(beginX, row2Y, middle, row2Y, progressLinePaint1);
        canvas.drawLine(middle, row2Y, endX, row2Y, progressLinePaint2);

        if (state.seeking) {
            float newMiddle = beginX + (endX - beginX) * state.newPosition;
            canvas.drawLine(beginX, row2Y, newMiddle, row2Y, progressLinePaint3);
        }
    }

    private float fade = 1f;

    public void setFade(float value) {
        if (value < 0f) {
            value = 0f;
        }
        if (value > 1f) {
            value = 1f;
        }
        fade = value;
    }

    private void applyFade() {
        int a = (int) (255 * fade);
        leftAlignTextPaint.setAlpha(a);
        rightAlignTextPaint.setAlpha(a);
        centerAlignTextPaint.setAlpha(a);
        activeTextPaint.setAlpha(a);
        progressLinePaint1.setAlpha(a);
        progressLinePaint2.setAlpha(a);
        progressLinePaint3.setAlpha(a);
        hoverBgPaint.setAlpha((int) (0x55 * fade));
        dwellRingPaint.setAlpha(a);
        dwellProgressPaint.setAlpha(a);
        cursorPaint.setAlpha(a);
        cursorOutlinePaint.setAlpha(a);
    }

    public void captureHome(float[] headView) {
        Matrix.transposeM(worldRecenter, 0, headView, 0);
        worldRecenter[3] = 0f;
        worldRecenter[7] = 0f;
        worldRecenter[11] = 0f;
        gazeCaptured = true;
        resetGaze();
    }

    public boolean isGazeOnPanel() {
        return gazeOnPanel;
    }

    public boolean shouldShowUI() {
        return gazeOnPanel && (System.nanoTime() - gazeOnPanelSinceNs) > SHOW_UI_DELAY_NS;
    }

    public float getGazeProgress() {
        float position = (gazeCanvasX - beginX) / (endX - beginX);
        if (position < 0f) {
            position = 0f;
        }
        if (position > 1f) {
            position = 1f;
        }
        return position;
    }

    private void resetGaze() {
        gazeOnPanel = false;
        gazeTarget = TARGET_NONE;
        gazeFiredTarget = TARGET_NONE;
        gazeDwell = 0f;
    }

    public int updateGaze(float[] headView, long nowNs) {
        if (!gazeCaptured || !state.videoLoaded || (state.errorMessage != null)) {
            resetGaze();
            return TARGET_NONE;
        }

        Matrix.multiplyMM(gazeView, 0, headView, 0, worldRecenter, 0);

        if (!Matrix.invertM(gazeInverse, 0, gazeView, 0)) {
            resetGaze();
            return TARGET_NONE;
        }

        Matrix.multiplyMV(gazeOrigin, 0, gazeInverse, 0, GAZE_ORIGIN, 0);
        Matrix.multiplyMV(gazeDirection, 0, gazeInverse, 0, GAZE_FORWARD, 0);

        boolean onPanel = false;
        float canvasX = 0f;
        float canvasY = 0f;
        if (Math.abs(gazeDirection[2]) > 1e-6f) {
            float t = (-panelDistance - gazeOrigin[2]) / gazeDirection[2];
            if (t > 0f) {
                float x = gazeOrigin[0] + t * gazeDirection[0];
                float y = gazeOrigin[1] + t * gazeDirection[1];
                if ((x >= panelLeft) && (x <= panelLeft + panelWidth)
                        && (y <= panelTop) && (y >= panelTop - panelHeight)) {
                    onPanel = true;
                    canvasX = (x - panelLeft) * PIXELS_PER_METER;
                    canvasY = (panelTop - y) * PIXELS_PER_METER;
                }
            }
        }

        int target = onPanel ? getTargetAt(canvasX, canvasY) : TARGET_NONE;

        if (onPanel && !gazeOnPanel) {
            gazeOnPanelSinceNs = nowNs;
        }
        gazeOnPanel = onPanel;
        gazeCanvasX = canvasX;
        gazeCanvasY = canvasY;

        if (target != gazeTarget) {
            gazeTarget = target;
            gazeDwell = 0f;
            gazeDwellStartNs = nowNs;
            if (target != gazeFiredTarget) {
                gazeFiredTarget = TARGET_NONE;
            }
        }

        int fired = TARGET_NONE;
        if (gazeTarget != TARGET_NONE) {
            if (gazeTarget == gazeFiredTarget) {
                gazeDwell = 1f;
            } else {
                gazeDwell = Math.min(1f,
                        (nowNs - gazeDwellStartNs) / (DWELL_MS * 1000000f));
                if (gazeDwell >= 1f) {
                    fired = gazeTarget;
                    gazeFiredTarget = gazeTarget;
                    gazeDwell = 0f;
                    gazeDwellStartNs = nowNs;
                }
            }
        }
        return fired;
    }

    private int getTargetAt(float canvasX, float canvasY) {
        if ((canvasY >= BUTTON_TOP) && (canvasY <= BUTTON_BOTTOM)) {
            for (int i = 0; i < BUTTON_CENTERS.length; i++) {
                if (Math.abs(canvasX - BUTTON_CENTERS[i]) <= BUTTON_HALF_WIDTH) {
                    return BUTTON_TARGETS[i];
                }
            }
        }
        if ((canvasY >= PROGRESS_TOP) && (canvasY <= PROGRESS_BOTTOM)
                && (canvasX >= beginX) && (canvasX <= endX)) {
            return TARGET_PROGRESS;
        }
        return TARGET_NONE;
    }

    private void drawGaze(Canvas canvas) {
        if (!gazeOnPanel) {
            return;
        }

        if (gazeTarget == TARGET_PROGRESS) {
            canvas.drawLine(gazeCanvasX, PROGRESS_TOP + 6, gazeCanvasX, PROGRESS_BOTTOM - 6,
                    progressLinePaint3);
            if (state.videoLength > 0) {
                drawString(canvas,
                        formatTime((int) (state.videoLength * getGazeProgress())),
                        gazeCanvasX, PROGRESS_TOP - 12, centerAlignTextPaint);
            }
        }

        if (gazeTarget != TARGET_NONE) {
            canvas.drawArc(gazeCanvasX - 26, gazeCanvasY - 26,
                    gazeCanvasX + 26, gazeCanvasY + 26, 0, 360, false, dwellRingPaint);
            if (gazeDwell > 0f) {
                canvas.drawArc(gazeCanvasX - 26, gazeCanvasY - 26,
                        gazeCanvasX + 26, gazeCanvasY + 26, -90, 360 * gazeDwell, false,
                        dwellProgressPaint);
            }
        }

        canvas.drawCircle(gazeCanvasX, gazeCanvasY, 7, cursorOutlinePaint);
        canvas.drawCircle(gazeCanvasX, gazeCanvasY, 4, cursorPaint);
    }

    private String formatTime(int ms) {
        int seconds = ms / 1000;
        int minutes = seconds / 60;
        int hours = minutes / 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds % 60);
        }
    }
}