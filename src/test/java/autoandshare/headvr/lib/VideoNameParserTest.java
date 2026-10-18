package autoandshare.headvr.lib;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VideoNameParserTest {

    private VideoType parseLayout(String name) {
        VideoType videoType = new VideoType();
        VideoNameParser.parseLayout(name, videoType);
        return videoType;
    }

    private VideoType parseType(String name) {
        VideoType videoType = new VideoType();
        VideoNameParser.parseType(name, videoType);
        return videoType;
    }

    @Test
    public void sideBySideTokens() {
        assertEquals(VideoType.Layout.SideBySide, parseLayout("Movie.3D.SBS.mkv").layout);
        assertEquals(VideoType.Layout.SideBySide, parseLayout("Movie.SBS.1080p.mp4").layout);
        assertEquals(VideoType.Layout.SideBySide, parseLayout("Movie.LR.mkv").layout);
        assertEquals(VideoType.Layout.SideBySide, parseLayout("Movie_3D_LR_5.7k.mp4").layout);
    }

    @Test
    public void topAndBottomTokens() {
        assertEquals(VideoType.Layout.TopAndBottom, parseLayout("Movie.OU.mkv").layout);
        assertEquals(VideoType.Layout.TopAndBottom, parseLayout("Movie.TAB.mp4").layout);
        assertEquals(VideoType.Layout.TopAndBottom, parseLayout("Movie.TB.mkv").layout);
        assertEquals(VideoType.Layout.TopAndBottom, parseLayout("Movie.3DTB.mkv").layout);
    }

    @Test
    public void halfAndFullAspects() {
        VideoType half = parseLayout("Movie.HSBS.mkv");
        assertEquals(VideoType.Layout.SideBySide, half.layout);
        assertEquals(VideoType.Aspect.Half, half.aspect);

        VideoType full = parseLayout("Movie.FSBS.mkv");
        assertEquals(VideoType.Layout.SideBySide, full.layout);
        assertEquals(VideoType.Aspect.Full, full.aspect);

        VideoType halfTab = parseLayout("Movie.HOU.mp4");
        assertEquals(VideoType.Layout.TopAndBottom, halfTab.layout);
        assertEquals(VideoType.Aspect.Half, halfTab.aspect);
    }

    @Test
    public void caseInsensitive() {
        assertEquals(VideoType.Layout.SideBySide, parseLayout("movie.sbs.mkv").layout);
        assertEquals(VideoType.Layout.SideBySide, parseLayout("MOVIE.SBS.MKV").layout);
        assertEquals(VideoType.Layout.TopAndBottom, parseLayout("movie.Tb.mkv").layout);
    }

    @Test
    public void noTokenStaysMono() {
        assertEquals(VideoType.Layout.Auto, parseLayout("download.mp4").layout);
        assertEquals(VideoType.Layout.Auto, parseLayout("TheBestOf_2019.mp4").layout);
        assertEquals(VideoType.Layout.Auto, parseLayout("h264_clip.mkv").layout);
        assertEquals(VideoType.Layout.Auto, parseLayout(null).layout);
    }

    @Test
    public void vr180And360() {
        assertEquals(VideoType.Type.VR180, parseType("Movie_180.mp4").type);
        assertEquals(VideoType.Type.VR180, parseType("Movie.180.SBS.3D.mp4").type);
        assertEquals(VideoType.Type.VR360, parseType("Movie_360_TB.mp4").type);
        assertEquals(VideoType.Type.Auto, parseType("Movie_18000.mp4").type);
        assertEquals(VideoType.Type.Auto, parseType(null).type);
    }

    @Test
    public void fisheyeTokens() {
        assertEquals(VideoType.Type.Fisheye180, parseType("Movie_180_fisheye_SBS.mp4").type);
        assertEquals(VideoType.Type.Fisheye180, parseType("clip.MKX200.mp4").type);
        assertEquals(VideoType.Type.Fisheye180, parseType("clip_vrca220.mp4").type);
        assertEquals(VideoType.Type.Fisheye180Equisolid, parseType("clip.equisolid.mp4").type);
        assertEquals(VideoType.Type.Auto, parseType("myfisheye.mp4").type);
        assertEquals(VideoType.Type.Auto, parseType(null).type);
    }
}
