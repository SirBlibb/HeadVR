package autoandshare.headvr.lib;

import android.util.Log;

import org.videolan.libvlc.MediaPlayer;

import autoandshare.headvr.lib.rendering.ContentForTwoEyes;
import autoandshare.headvr.lib.rendering.Mesh;

public class State {
    private static final String TAG = "VideoState";

    // list info
    public String indexString;

    // player ready
    public boolean videoLoaded;
    public boolean readyToDraw;

    // error info
    public String errorMessage;

    public boolean isFileProtocol;

    // basic video info
    public String fileName;
    public String layoutInfo;

    // loaded info
    public String title;
    public MediaPlayer.TrackDescription[] audioTracks;
    public MediaPlayer.TrackDescription[] subtitleTracks;

    // playing and seek info
    public boolean playing;
    public boolean seeking;
    public boolean forward;
    public int videoLength;
    public int currentTime;
    public float currentPosition;
    public float newPosition;

    // optional info
    public String message;
    public String playerState;

    // motions
    public String motions;

    // Properties
    public String propertyKey;
    public boolean force2D;
    public VideoType videoType;

    //
    public int mediaType;

    public boolean isStillImage;

    public void setForce2D(boolean force2D) {
        this.force2D = force2D;
        VideoProperties.setForce2D(propertyKey, force2D);
        ContentForTwoEyes.force2D = force2D;
    }

    public void loadValues() {
        this.force2D = VideoProperties.getForce2D(propertyKey);
        ContentForTwoEyes.force2D = force2D;

        ContentForTwoEyes.EyeDistance3D = VideoProperties.getVideoEyeDistanceFloat(propertyKey);

        getVideoLayoutAndType();
        mediaType = getMediaFormatFromLayout(videoType.layout);
    }

    public void toggleForce2D() {
        if (this.videoLoaded && (this.videoType != null) && !this.videoType.isMono()) {
            this.force2D = !this.force2D;
            setForce2D(this.force2D);
        }
    }

    public boolean is2DContent() {
        return this.force2D || this.videoType.isMono();
    }

    public int getVideoEyeDistance() {
        if (is2DContent()) {
            return Setting.Instance.get(Setting.id.EyeDistance);
        } else {
            return VideoProperties.getVideoEyeDistance(propertyKey);
        }
    }

    public void setVideoEyeDistance(int newVal) {
        if (force2D || videoType.isMono()) {
            Setting.Instance.set(Setting.id.EyeDistance, newVal);
        } else {
            VideoProperties.setVideoEyeDistance(propertyKey, newVal);
            ContentForTwoEyes.EyeDistance3D = VideoProperties.getVideoEyeDistanceFloat(propertyKey);
        }
    }

    public int updateVideoEyeDistance(int offset) {
        int newVal = offset + VideoProperties.getVideoEyeDistance(propertyKey);
        setVideoEyeDistance(newVal);
        return newVal;
    }

    private Setting setting;

    public State(Setting setting) {
        this.setting = setting;
    }

    public void reset() {
        this.videoLoaded = false;
        this.readyToDraw = false;

        this.errorMessage = null;

        this.fileName = "";

        this.title = "";
        this.audioTracks = null;
        this.subtitleTracks = null;

        this.videoType = null;
        this.force2D = false;
        this.isStillImage = false;

        this.playing = false;
        this.seeking = false;
        this.forward = false;
        this.videoLength = 0;
        this.currentTime = 0;
        this.currentPosition = 0;
        this.newPosition = 0;

        this.message = null;
        this.playerState = null;
    }

    public boolean normalPlaying() {
        return this.videoLoaded &&
                this.playing && (!this.seeking);
    }

    private int getMediaFormatFromLayout(VideoType.Layout layout) {
        switch (layout) {
            case SideBySide:
                return Mesh.MEDIA_STEREO_LEFT_RIGHT;
            case TopAndBottom:
                return Mesh.MEDIA_STEREO_TOP_BOTTOM;
        }
        return Mesh.MEDIA_MONOSCOPIC;
    }

    private void getVideoLayout(String[] infos, VideoType videoType) {
        int i = 0;
        while (videoType.layout == VideoType.Layout.Auto && i < infos.length) {
            VideoNameParser.parseLayout(infos[i], videoType);
            i += 1;
        }

        if (videoType.layout == VideoType.Layout.Auto) {
            videoType.layout = VideoType.Layout.Mono;
        }
    }

    private void getVideoType(String[] infos, VideoType videoType) {
        int i = 0;
        while (videoType.type == VideoType.Type.Auto && i < infos.length) {
            VideoNameParser.parseType(infos[i], videoType);
            i += 1;
        }

        if (videoType.type == VideoType.Type.Auto) {
            videoType.type = VideoType.Type.Plane;
        }
    }

    private void getVideoLayoutAndType() {
        videoType = VideoProperties.getVideoType(propertyKey);

        getVideoLayout(new String[] {layoutInfo, fileName, title}, videoType);

        getVideoType(new String[] {layoutInfo, fileName, title}, videoType);

        if ((videoType.type == VideoType.Type.Plane)
                && (videoType.layout == VideoType.Layout.Mono)) {
            applyDefaultVideoLayout(videoType);
        }

        Log.d(TAG, "Video Type: " + videoType);
    }

    private void applyDefaultVideoLayout(VideoType videoType) {
        String value = setting.getString(Setting.id.DefaultVideoLayout);
        switch (value) {
            case "VR180_SBS":
                videoType.type = VideoType.Type.VR180;
                videoType.layout = VideoType.Layout.SideBySide;
                break;
            case "VR180_TAB":
                videoType.type = VideoType.Type.VR180;
                videoType.layout = VideoType.Layout.TopAndBottom;
                break;
            case "VR360_SBS":
                videoType.type = VideoType.Type.VR360;
                videoType.layout = VideoType.Layout.SideBySide;
                break;
            case "VR360_TAB":
                videoType.type = VideoType.Type.VR360;
                videoType.layout = VideoType.Layout.TopAndBottom;
                break;
            default:
                break;
        }
    }

    public int getSubtitle() {
        return VideoProperties.getVideoSubtitle(propertyKey);
    }

    public int getAudio() {
        return VideoProperties.getVideoAudio(propertyKey);
    }

    public float getPosition() {
        float position = VideoProperties.getPosition(propertyKey);
        Log.d(TAG, "got position " + position);
        return position;
    }

    public void setPosition(float v) {
        Log.d(TAG, "saved position " + v);
        VideoProperties.setPosition(propertyKey, v);
    }
}
