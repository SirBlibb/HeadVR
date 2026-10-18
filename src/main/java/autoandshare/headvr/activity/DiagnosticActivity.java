package autoandshare.headvr.activity;

import android.content.pm.ActivityInfo;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

import autoandshare.headvr.R;
import autoandshare.headvr.lib.AlignmentUI;
import autoandshare.headvr.lib.Setting;
import autoandshare.headvr.lib.rendering.VRTexture2D;

public class DiagnosticActivity extends GvrActivity implements
        GvrView.StereoRenderer {

    private static final String TAG = "DiagnosticActivity";

    private Setting setting;
    private AlignmentUI alignUI;
    private boolean homeCaptured = false;
    private volatile boolean recenterRequested = false;
    private final float[] latestHeadView = new float[16];
    private long lastTapTime = 0L;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        setting = new Setting(this);

        if (!setting.getBoolean(Setting.id.LandscapeNormal)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        }

        setContentView(R.layout.activity_diagnostic);

        GvrView cardboardView = findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        cardboardView.setOnCardboardTriggerListener(this::onTap);
        this.setGvrView(cardboardView);
    }

    private void onTap() {
        if (alignUI == null) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastTapTime < 350) {
            // undo the single tap that started this double tap, then toggle
            alignUI.prevPattern();
            alignUI.toggleAnchor();
            recenterRequested = true;
            lastTapTime = 0L;
        } else {
            alignUI.nextPattern();
            lastTapTime = now;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            onTap();
        }
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int delta = 0;
            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                delta = 1;
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
                delta = -1;
            }
            if (delta != 0) {
                setting.update(Setting.id.EyeDistance, delta);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        headTransform.getHeadView(latestHeadView, 0);
        if (alignUI != null) {
            if (!homeCaptured || recenterRequested) {
                alignUI.captureHome(latestHeadView);
                homeCaptured = true;
                recenterRequested = false;
            }
        }
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (alignUI != null) {
            alignUI.glDraw(eye);
        }
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        VRTexture2D.glInit();
        GLES20.glClearColor(0f, 0f, 0f, 1f);

        alignUI = new AlignmentUI();
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }
}
