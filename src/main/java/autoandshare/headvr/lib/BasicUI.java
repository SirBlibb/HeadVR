package autoandshare.headvr.lib;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
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
    public static final int TARGET_SHOW_UI = 8;
    public static final int TARGET_SUMMON_UI = 9;

    private static final int DWELL_MS = 2000;
    private static final long GAZE_SHOW_DELAY_NS = 350000000L;
    private static final long SUMMON_DELAY_NS = 300000000L;
    // Vertical placement of the control bar relative to the view direction at the
    // moment it is shown. Keeping this constant means the bar always pops up in the
    // same place on screen instead of jumping around. 23 degrees puts it just below
    // a normal 16:9 video screen, while still comfortably inside the field of view.
    private static final float SHOW_TOP_DEGREES = 23f;
    // Looking further down than this (while the bar is hidden) brings it back.
    private static final float SUMMON_PITCH_DEGREES = 26f;
    // The head has to come back above this angle before a new look-down summon can
    // fire, so the bar does not instantly reappear while it is fading out.
    private static final float SUMMON_REARM_PITCH_DEGREES = 12f;
    // Keep an existing bar position while it is still comfortably in view.
    private static final float KEEP_IN_VIEW_H_DEGREES = 45f;
    private static final float KEEP_IN_VIEW_V_DEGREES = 38f;
    private static final float PIXELS_PER_METER = 360f;
    private static final float HINT_CENTER_X = 460f;
    private static final float HINT_CENTER_Y = 69f;
    private static final float HINT_HALF_WIDTH = 42f;
    private static final float HINT_HALF_HEIGHT = 28f;
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

    private boolean lastUiVisible = false;
    private boolean gazeRearmed = false;
    private boolean lookDownRearmed = false;
    private long lookDownStartNs = 0;

    private final float[] panelView = new float[16];
    private final float[] panelPoint = new float[4];
    private final float[] panelPointView = new float[4];

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

        hintBgPaint = new Paint();
        hintBgPaint.setColor(0x99000000);

        hintTextPaint = new TextPaint();
        hintTextPaint.setColor(0xCCDDDDDD);
        hintTextPaint.setTextSize(30);
        hintTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    private Paint hoverBgPaint;
    private Paint dwellRingPaint;
    private Paint dwellProgressPaint;
    private Paint cursorPaint;
    private Paint cursorOutlinePaint;
    private TextPaint activeTextPaint;
    private Paint hintBgPaint;
    private TextPaint hintTextPaint;

    private void initSurface(float widthPixel, float heightPixel) {
        float uiWidth = widthPixel / PIXELS_PER_METER;
        float uiHeight = heightPixel / PIXELS_PER_METER;
        panelLeft = -uiWidth / 2;
        panelTop = topForDegrees(SHOW_TOP_DEGREES);
        panelWidth = uiWidth;
        panelHeight = uiHeight;
        init(uiWidth, uiHeight, panelDistance,
                new PointF(panelLeft, panelTop),
                (int) widthPixel, (int) heightPixel);
        worldAnchored = true;
        worldRecenter = new float[16];
    }

    private float topForDegrees(float degrees) {
        return (float) (-panelDistance * Math.tan(Math.toRadians(degrees)));
    }

    private void setPanelTopDegrees(float degrees) {
        float newTop = topForDegrees(degrees);
        if (Math.abs(newTop - panelTop) < 0.0001f) {
            return;
        }
        panelTop = newTop;
        updatePositions(panelWidth, panelHeight, panelDistance,
                new PointF(panelLeft, panelTop));
    }

    public void glDraw(Eye eye) {
        drawCanvas(eye, false);
    }

    public void glDrawCollapsed(Eye eye) {
        drawCanvas(eye, true);
    }

    private void drawCanvas(Eye eye, boolean collapsed) {
        if (eye.getType() == 1) {
            Canvas canvas = this.getCanvas();

            if (collapsed) {
                drawCollapsedHint(canvas);
            } else {
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
            }

            this.releaseCanvas(canvas);
        }
        this.draw(eye);
    }

    private void drawCollapsedHint(Canvas canvas) {
        // The collapsed bar is mostly transparent; only a small menu tab stays
        // visible so the controls can always be found again.
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        float centerX = HINT_CENTER_X;
        float centerY = HINT_CENTER_Y;
        canvas.drawRoundRect(centerX - 34, centerY - 22,
                centerX + 34, centerY + 22, 12, 12, hintBgPaint);
        drawString(canvas, "\u2630", centerX, centerY, hintTextPaint);
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
        if (state.isStillImage) {
            time = "Photo";
        } else if (state.videoLength > 0) {
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

    /**
     * Positions the control bar for the current view. The bar is only moved when
     * it would otherwise be out of sight (or when force is set, e.g. after a
     * recenter or a new video), so it stays put while it is being used.
     */
    public void placeForShow(float[] headView, boolean force) {
        if (force || !gazeCaptured || !isPanelInView(headView)) {
            Matrix.transposeM(worldRecenter, 0, headView, 0);
            worldRecenter[3] = 0f;
            worldRecenter[7] = 0f;
            worldRecenter[11] = 0f;
            gazeCaptured = true;
            setPanelTopDegrees(SHOW_TOP_DEGREES);
        }
        resetGazeTargets();
    }

    /**
     * While the bar is collapsed its small menu tab should remain reachable.
     * If the user has turned away from it, quietly bring it back in front of
     * them (only the tab is visible in this state).
     */
    public void keepCollapsedInView(float[] headView) {
        if (!gazeCaptured || !isPanelInView(headView)) {
            placeForShow(headView, true);
        }
    }

    private boolean isPanelInView(float[] headView) {
        if (!gazeCaptured) {
            return false;
        }
        Matrix.multiplyMM(panelView, 0, headView, 0, worldRecenter, 0);
        panelPoint[0] = 0f;
        panelPoint[1] = panelTop - panelHeight / 2f;
        panelPoint[2] = -panelDistance;
        panelPoint[3] = 1f;
        Matrix.multiplyMV(panelPointView, 0, panelView, 0, panelPoint, 0);

        if (panelPointView[2] >= -0.01f) {
            return false;
        }
        float depth = -panelPointView[2];
        float horizontal = Math.abs(panelPointView[0]) / depth;
        float vertical = Math.abs(panelPointView[1]) / depth;
        return horizontal <= Math.tan(Math.toRadians(KEEP_IN_VIEW_H_DEGREES))
                && vertical <= Math.tan(Math.toRadians(KEEP_IN_VIEW_V_DEGREES));
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

    public boolean isGazeOnMenu() {
        return lastUiVisible && gazeOnPanel;
    }

    public int updateGaze(float[] headView, float[] headForward, long nowNs, boolean uiVisible) {
        if (uiVisible != lastUiVisible) {
            lastUiVisible = uiVisible;
            resetGaze();
        }

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

        gazeOnPanel = onPanel;
        gazeCanvasX = canvasX;
        gazeCanvasY = canvasY;

        if (!uiVisible) {
            boolean onIcon = onPanel
                    && (Math.abs(canvasX - HINT_CENTER_X) <= HINT_HALF_WIDTH)
                    && (Math.abs(canvasY - HINT_CENTER_Y) <= HINT_HALF_HEIGHT);
            gazeTarget = TARGET_NONE;
            gazeDwell = 0f;
            return updateCollapsed(headForward, onIcon, nowNs);
        }

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

    private int updateCollapsed(float[] headForward, boolean onIcon, long nowNs) {
        // A fresh look at the menu tab, or a deliberate look down, brings the
        // full bar back. The re-arm flags stop it from instantly reappearing
        // while the user is still looking at the place it disappeared from.
        if (!onIcon) {
            gazeRearmed = true;
            gazeOnPanelSinceNs = 0;
        } else if (gazeRearmed) {
            if (gazeOnPanelSinceNs == 0) {
                gazeOnPanelSinceNs = nowNs;
            } else if (nowNs - gazeOnPanelSinceNs >= GAZE_SHOW_DELAY_NS) {
                resetGaze();
                return TARGET_SHOW_UI;
            }
        }

        float forwardY = (headForward != null) ? headForward[1] : 0f;
        boolean lookingUp = forwardY > -Math.sin(Math.toRadians(SUMMON_REARM_PITCH_DEGREES));
        boolean lookingDown = forwardY < -Math.sin(Math.toRadians(SUMMON_PITCH_DEGREES));

        if (lookingUp) {
            lookDownRearmed = true;
        }
        if (lookingDown && lookDownRearmed) {
            if (lookDownStartNs == 0) {
                lookDownStartNs = nowNs;
            } else if (nowNs - lookDownStartNs >= SUMMON_DELAY_NS) {
                resetGaze();
                return TARGET_SUMMON_UI;
            }
        } else {
            lookDownStartNs = 0;
        }

        return TARGET_NONE;
    }

    private void resetGaze() {
        resetGazeTargets();
        gazeRearmed = false;
        lookDownRearmed = false;
        lookDownStartNs = 0;
    }

    private void resetGazeTargets() {
        gazeOnPanel = false;
        gazeTarget = TARGET_NONE;
        gazeFiredTarget = TARGET_NONE;
        gazeDwell = 0f;
        gazeDwellStartNs = 0;
        gazeOnPanelSinceNs = 0;
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