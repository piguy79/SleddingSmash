package com.railwaygames.sleddingsmash;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class SSTexturePacker {

    public static final void main(String[] args) {
        TexturePacker.process("texturepacker/images/menus", "android/assets/data/images", "menus");
    }
}
