package autoandshare.headvr.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.videolan.libvlc.MediaPlayer;

import java.util.Locale;

import autoandshare.headvr.R;
import autoandshare.headvr.lib.Setting;
import autoandshare.headvr.lib.State;
import autoandshare.headvr.lib.VideoProperties;
import autoandshare.headvr.lib.VideoRenderer;
import autoandshare.headvr.lib.VideoType;

public class VideoOptions extends AppCompatActivity {
    private static final String TAG = "VideoOptions";

    public static State _state; // a workaround to pass object to activity
    private State state; // capture the value here
    private String propertyKey;

    private void setFontSize() {
        Configuration configuration = this.getResources().getConfiguration();
        configuration.fontScale = 0.8f;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    private void addRadioButton(RadioGroup radioGroup, String text, boolean checked, Runnable work) {
        RadioButton radioButton = new RadioButton(this);
        radioButton.setText(text);
        radioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                work.run();
            }
        });
        radioGroup.addView(radioButton);
        radioButton.setChecked(checked); // this should be after "addView"
    }

    private void initTrackRadioGroup(RadioGroup group, int pref,
                                     MediaPlayer.TrackDescription[] tracks,
                                     VideoActivity.Consumer<Integer> putVal) {
        if (tracks == null || tracks.length == 0) {
            return;
        }

        addRadioButton(group, "Auto", pref == VideoProperties.TrackAuto,
                () -> putVal.accept(VideoProperties.TrackAuto));
        for (MediaPlayer.TrackDescription t : tracks) {
            addRadioButton(group, t.name, pref == t.id,
                    () -> putVal.accept(t.id));
        }
    }

    private interface Labeler<T> {
        String label(T value);
    }

    private <T> void initEnumRadioGroup(RadioGroup radioGroup, T[] values, Labeler<T> labeler,
                                        T selection, VideoActivity.Consumer<T> putVal) {
        for (T val : values) {
            addRadioButton(radioGroup, labeler.label(val), val.equals(selection),
                    () -> putVal.accept(val));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFontSize();

        setContentView(R.layout.activity_video_options);

        this.state = VideoOptions._state;
        this.propertyKey = state.propertyKey;

        VideoType videoType = VideoProperties.getVideoType(propertyKey);
        initEnumRadioGroup(findViewById(R.id.radio_group_video_type),
                VideoType.Type.values(),
                VideoType.Type::label,
                videoType.type,
                (x) -> {
                    videoType.type = x;
                    VideoProperties.setVideoType(propertyKey, videoType);
                    updateModeHint();
                }
        );
        initEnumRadioGroup(findViewById(R.id.radio_group_video_type_layout),
                VideoType.Layout.values(),
                VideoType.Layout::label,
                videoType.layout,
                (x) -> {
                    videoType.layout = x;
                    VideoProperties.setVideoType(propertyKey, videoType);
                    updateModeHint();
                }
        );
        initEnumRadioGroup(findViewById(R.id.radio_group_video_type_aspect),
                VideoType.Aspect.values(),
                VideoType.Aspect::label,
                videoType.aspect,
                (x) -> {
                    videoType.aspect = x;
                    VideoProperties.setVideoType(propertyKey, videoType);
                    updateModeHint();
                }
        );
        initTrackRadioGroup(findViewById(R.id.radio_group_audio_tracks),
                VideoProperties.getVideoAudio(propertyKey),
                state.audioTracks,
                (x) -> VideoProperties.setVideoAudio(propertyKey, x)
        );
        initTrackRadioGroup(findViewById(R.id.radio_group_subtitle_tracks),
                VideoProperties.getVideoSubtitle(propertyKey),
                state.subtitleTracks,
                (x) -> VideoProperties.setVideoSubtitle(propertyKey, x)
        );

        Switch softwareDecodingSwitch = findViewById(R.id.softwareDecodingSwitch);
        softwareDecodingSwitch.setChecked(VideoProperties.getVideoSoftwareDecoding(propertyKey));
        softwareDecodingSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                VideoProperties.setVideoSoftwareDecoding(propertyKey, isChecked));

        initSpeedSeekBar();
        initDelaySeekBar(R.id.seekBarAudioDelay, R.id.textViewAudioDelay, "Audio delay",
                -2000, 50, VideoProperties.getVideoAudioDelay(propertyKey),
                (x) -> VideoProperties.setVideoAudioDelay(propertyKey, x));
        initDelaySeekBar(R.id.seekBarSubtitleDelay, R.id.textViewSubtitleDelay, "Subtitle delay",
                -5000, 100, VideoProperties.getVideoSpuDelay(propertyKey),
                (x) -> VideoProperties.setVideoSpuDelay(propertyKey, x));

        updateModeHint();

        updateOptionView();
    }

    private void updateModeHint() {
        TextView hint = findViewById(R.id.textViewDetectedMode);
        VideoType stored = VideoProperties.getVideoType(propertyKey);
        boolean auto = (stored.type == VideoType.Type.Auto)
                && (stored.layout == VideoType.Layout.Auto)
                && (stored.aspect == VideoType.Aspect.Auto);
        if (auto && (state.videoType != null)) {
            hint.setText("In use: "
                    + state.videoType.type.label() + ", "
                    + state.videoType.layout.label() + ", "
                    + state.videoType.aspect.label()
                    + " (auto-detected)");
        } else {
            hint.setText("Saved for this video: "
                    + stored.type.label() + ", "
                    + stored.layout.label() + ", "
                    + stored.aspect.label());
        }
    }

    private void updateOptionView() {
        Switch force2DSwitch = findViewById(R.id.force2DSwitch);
        if (state.videoType.isMono()) {
            force2DSwitch.setVisibility(View.GONE);
        } else {
            force2DSwitch.setVisibility(View.VISIBLE);
            force2DSwitch.setChecked(state.force2D);
            force2DSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    state.setForce2D(isChecked);
                }
            });

        }
        if (state.is2DContent()) {
            ((TextView) findViewById(R.id.textViewEyeDistance)).setText("Default Eye Distance is used for 2D content");
            findViewById(R.id.seekBarEyeDistance).setVisibility(View.INVISIBLE);
        } else {
            SettingFragment.initSeekBar(findViewById(android.R.id.content),
                    R.id.seekBarEyeDistance, Setting.id.EyeDistance, R.id.textViewEyeDistance,
                    state.getVideoEyeDistance(),
                    (x) -> state.setVideoEyeDistance(x),
                    "%s for this video (%d)");
        }
    }

    private void initSpeedSeekBar() {
        SeekBar seekBar = findViewById(R.id.seekBarPlaybackSpeed);
        seekBar.setMax(7);
        seekBar.setProgress(Math.round(VideoProperties.getVideoRate(propertyKey) / 25f) - 1);
        updateSpeedLabel(VideoProperties.getVideoRate(propertyKey));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int ratePercent = (progress + 1) * 25;
                VideoProperties.setVideoRate(propertyKey, ratePercent);
                updateSpeedLabel(ratePercent);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
    }

    private void updateSpeedLabel(int ratePercent) {
        String text = (ratePercent % 100 == 0) ?
                (ratePercent / 100) + "x speed" :
                String.format(Locale.US, "%.2fx speed", ratePercent / 100f);
        ((TextView) findViewById(R.id.textViewPlaybackSpeed)).setText(text);
    }

    private void initDelaySeekBar(int seekBarId, int textViewId, String label,
                                  int minMs, int stepMs, int initialMs,
                                  VideoActivity.Consumer<Integer> putVal) {
        int center = -minMs / stepMs;
        SeekBar seekBar = findViewById(seekBarId);
        seekBar.setMax(center * 2);
        seekBar.setProgress(initialMs / stepMs + center);
        updateDelayLabel(textViewId, label, initialMs);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int delayMs = (progress - center) * stepMs;
                putVal.accept(delayMs);
                updateDelayLabel(textViewId, label, delayMs);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
    }

    private void updateDelayLabel(int textViewId, String label, int delayMs) {
        String value = ((delayMs > 0) ? "+" : "") + delayMs + " ms";
        ((TextView) findViewById(textViewId)).setText(label + ": " + value);
    }

    public void closeButtonPressed(View v) {
        finish();
    }
}