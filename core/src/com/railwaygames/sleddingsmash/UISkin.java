package com.railwaygames.sleddingsmash;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

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
        TextureAtlas menusAtlas = assetManager.get("data/images/menus.atlas", TextureAtlas.class);
        {
            NinePatchDrawable trd = new NinePatchDrawable(createNinePatch(menusAtlas.findRegion("button_clear")));
            add(Constants.UI.CLEAR_BUTTON, new Button.ButtonStyle(trd, trd, trd));
        }
        {
            TextureRegionDrawable trd = new TextureRegionDrawable(menusAtlas.findRegion("pause"));
            add(Constants.UI.PAUSE_BUTTON, new Button.ButtonStyle(trd, trd, trd));
        }
        {
            TextureRegionDrawable arrow = new TextureRegionDrawable(menusAtlas.findRegion("arrow_left"));
            add(Constants.UI.LEFT_BUTTON, new Button.ButtonStyle(arrow, arrow, arrow));
        }
        {
            TextureRegionDrawable arrow = new TextureRegionDrawable(menusAtlas.findRegion("arrow_right"));
            add(Constants.UI.RIGHT_BUTTON, new Button.ButtonStyle(arrow, arrow, arrow));
        }
        {
            TextureRegionDrawable arrow = new TextureRegionDrawable(menusAtlas.findRegion("arrow_up"));
            add(Constants.UI.UP_BUTTON, new Button.ButtonStyle(arrow, arrow, arrow));
        }
        {
            TextureRegionDrawable arrow = new TextureRegionDrawable(menusAtlas.findRegion("arrow_down"));
            add(Constants.UI.DOWN_BUTTON, new Button.ButtonStyle(arrow, arrow, arrow));
        }
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
