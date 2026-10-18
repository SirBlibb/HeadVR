package autoandshare.headvr.lib;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoNameParser {
    private static final Pattern layoutPattern =
            Pattern.compile("([^A-Za-z0-9]|^)(half|h|full|f|)[^A-Za-z0-9]?(3d)?(sbs|lr|ou|tab|tb)([^A-Za-z0-9]|$)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern vrPattern =
            Pattern.compile("([^A-Za-z0-9]|^)(180|360)([^A-Za-z0-9]|$)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern fisheyePattern =
            Pattern.compile("([^A-Za-z0-9]|^)(fisheye|mkx200|mkx220|vrca220|equisolid)([^A-Za-z0-9]|$)",
                    Pattern.CASE_INSENSITIVE);

    public static void parseLayout(String name, VideoType videoType) {
        if (name == null) {
            return;
        }
        Matcher matcher = layoutPattern.matcher(name);
        if (matcher.find()) {
            String aspect = matcher.group(2).toLowerCase();
            if (aspect.startsWith("h") && (videoType.aspect == VideoType.Aspect.Auto)) {
                videoType.aspect = VideoType.Aspect.Half;
            } else if (aspect.startsWith("f") && (videoType.aspect == VideoType.Aspect.Auto)) {
                videoType.aspect = VideoType.Aspect.Full;
            }

            String layout = matcher.group(4).toLowerCase();
            if (layout.startsWith("s") || layout.startsWith("l")) {
                videoType.layout = VideoType.Layout.SideBySide;
            } else {
                videoType.layout = VideoType.Layout.TopAndBottom;
            }
        }
    }

    public static void parseType(String name, VideoType videoType) {
        if (name == null) {
            return;
        }
        Matcher fisheye = fisheyePattern.matcher(name);
        if (fisheye.find()) {
            videoType.type = fisheye.group(2).equalsIgnoreCase("equisolid") ?
                    VideoType.Type.Fisheye180Equisolid : VideoType.Type.Fisheye180;
            return;
        }
        Matcher matcher = vrPattern.matcher(name);
        if (matcher.find()) {
            videoType.type = matcher.group(2).equals("180") ?
                    VideoType.Type.VR180 : VideoType.Type.VR360;
        }
    }
}
