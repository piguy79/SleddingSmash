package com.railwaygames.sleddingsmash.levels.modifiers;


import com.badlogic.gdx.graphics.g3d.Model;

import java.util.Map;

public interface TerrainModifier {

    /**
     * Given a model, perform some modification to the underlying meshes
     *
     * @param model  Model to modify in place
     * @param params Map of optional parameters to customize the modification
     */
    public void modify(Model model, Map<String, Object> params);
}
