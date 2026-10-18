package autoandshare.headvr.lib;

public class VideoType {

    public enum Type {
        Auto,
        Plane,
        VR180,
        VR360,
        Fisheye180,
        Fisheye180Equisolid;

        public String label() {
            switch (this) {
                case Plane:
                    return "Flat (2D)";
                case VR180:
                    return "VR180";
                case VR360:
                    return "VR360";
                case Fisheye180:
                    return "Fisheye 180 (equidistant)";
                case Fisheye180Equisolid:
                    return "Fisheye 180 (equisolid)";
            }
            return "Auto";
        }
    }

    public enum Layout {
        Auto,
        Mono,
        SideBySide,
        TopAndBottom;

        public String label() {
            switch (this) {
                case Mono:
                    return "Mono (2D)";
                case SideBySide:
                    return "Side by side";
                case TopAndBottom:
                    return "Top & bottom";
            }
            return "Auto";
        }
    }

    public enum Aspect {
        Auto,
        Half,
        Full;

        public String label() {
            switch (this) {
                case Half:
                    return "Half";
                case Full:
                    return "Full";
            }
            return "Auto";
        }
    }

    public Type type = Type.Auto;
    public Layout layout = Layout.Auto;
    public Aspect aspect = Aspect.Auto;

    public boolean isSBS() {
        return layout == Layout.SideBySide;
    }
    public boolean isTAB() {
        return layout == Layout.TopAndBottom;
    }
    public boolean isMono() {
        return layout == Layout.Mono;
    }
    public boolean isFull() {
        return aspect == Aspect.Full;
    }
    public boolean isHalf() {
        return aspect == Aspect.Half;
    }
    public boolean isVR() {
        return type != Type.Plane;
    }

    public boolean isFisheye() {
        return (type == Type.Fisheye180) || (type == Type.Fisheye180Equisolid);
    }

    @Override
    public String toString() {
        return "VideoType{" +
                "type=" + type +
                ", layout=" + layout +
                ", aspect=" + aspect +
                '}';
    }
}
