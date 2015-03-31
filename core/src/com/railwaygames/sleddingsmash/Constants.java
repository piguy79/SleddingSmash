package com.railwaygames.sleddingsmash;

import java.text.SimpleDateFormat;

public class Constants {
    public static final String APP_TITLE = "Sledding Smash";
    public static final String PREFERENCE_STORE = "SleddingSmash";
    public static SimpleDateFormat timeRemainingFormat = new SimpleDateFormat("mm:ss");

    public static class UI {
        public static final String X_SMALL_FONT = "xSmallFont";
        public static final String SMALL_FONT = "smallFont";
        public static final String DEFAULT_FONT = "defaultFont";
        public static final String MEDIUM_LARGE_FONT = "mediumLargeFont";
        public static final String LARGE_FONT = "largeFont";
        public static final String X_LARGE_FONT = "xLargeFont";

        public static final String CLEAR_BUTTON = "clearButton";
        public static final String PAUSE_BUTTON = "pauseButton";
        public static final String UP_BUTTON = "upButton";
        public static final String DOWN_BUTTON = "downButton";
    }

    public static class CharacterState {
        public static final String SLEEP = "sleep";
        public static final String VICTORY = "victory";
    }

    public static class CollisionsFlag {
        public static final int SPHERE_FLAG = 2;
        public static final int TREE_FLAG = 3;
        public static final int PLANE_FLAG = 4;
        public static final int STAR_FLAG = 5;

    }

    public static class Colors {

    }
}
