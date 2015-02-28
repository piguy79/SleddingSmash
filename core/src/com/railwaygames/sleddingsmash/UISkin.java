package com.railwaygames.sleddingsmash;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class UISkin extends Skin {

    private AssetManager assetManager;

    @Override
    public BitmapFont getFont(String name) {
        if (name.equals(Constants.UI.X_LARGE_FONT)) {
            return Fonts.getInstance(assetManager).xLargeFont();
        } else if (name.equals(Constants.UI.LARGE_FONT)) {
            return Fonts.getInstance(assetManager).largeFont();
        } else if (name.equals(Constants.UI.X_SMALL_FONT)) {
            return Fonts.getInstance(assetManager).xSmallFont();
        } else if (name.equals(Constants.UI.MEDIUM_LARGE_FONT)) {
            return Fonts.getInstance(assetManager).mediumLargeFont();
        } else if (name.equals(Constants.UI.DEFAULT_FONT)) {
            return Fonts.getInstance(assetManager).mediumFont();
        } else if (name.equals(Constants.UI.SMALL_FONT)) {
            return Fonts.getInstance(assetManager).smallFont();
        }
        return super.getFont(name);
    }

    public void initialize(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    private NinePatch createNinePatch(AtlasRegion region) {
        int[] splits = region.splits;
        NinePatch patch = null;
        if (splits != null) {
            patch = new NinePatch(region, splits[0], splits[1], splits[2], splits[3]);
            int[] pads = ((AtlasRegion) region).pads;
            if (pads != null)
                patch.setPadding(pads[0], pads[1], pads[2], pads[3]);
        }

        return patch;
    }
}