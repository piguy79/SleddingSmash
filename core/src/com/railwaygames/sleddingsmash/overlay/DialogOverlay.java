package com.railwaygames.sleddingsmash.overlay;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.railwaygames.sleddingsmash.Resources;

public class DialogOverlay extends Overlay {

    public DialogOverlay(Resources resources) {
        super(resources);
        this.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
            }
        });
    }

}
