package com.railwaygames.sleddingsmash.levels.obstacles;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.railwaygames.sleddingsmash.entity.GameObject;
import com.railwaygames.sleddingsmash.utils.MapUtils;
import com.railwaygames.sleddingsmash.utils.MathUtils;
import com.railwaygames.sleddingsmash.utils.ModelUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by conormullen on 2/19/15.
 */
public abstract class ObstacleGenerator {

    Model model;

    public static final String START_X = "startX";
    public static final String END_X = "endX";
    public static final String START_Z = "startZ";
    public static final String END_Z = "endZ";
    public static final String DENSITY = "density";
    public static final String ANGLE = "angle";
    public static final String MODEL = "model";

    private Map<String, Object> defaultParams = new HashMap<String, Object>() {{
        put(START_X, 0.2f);
        put(END_X, 0.6f);
        put(START_Z, 0.01f);
        put(END_Z, 0.1f);
        put(DENSITY, 20f);
        put(ANGLE, 60f);
        put(MODEL, "tree");
    }};

    protected ObstacleGenerator(Model model) {
        this.model = model;
    }

    public List<GameObject> generateObstacles(Model areaModel, Map<String, Object> params, Vector3 upVector, Vector3 offset) {
        MapUtils.addDefaults(params, defaultParams);

        ModelUtils.RectangleArea area = new ModelUtils.RectangleArea((Float) params.get(START_X), (Float) params.get(START_Z), (Float) params.get(END_X), (Float) params.get(END_Z));
        List<Vector3> locations = ModelUtils.findAreaInModel(areaModel, area, upVector, (Float) params.get(ANGLE));

        return generateObstacles(params, upVector, offset, locations);
    }

    private List<GameObject> generateObstacles(Map<String, Object> params, Vector3 upVector, Vector3 offset, List<Vector3> locations) {
        List<GameObject> gameObjects = new ArrayList<GameObject>();
        List<Integer> usedIndexes = new ArrayList<Integer>();

        if (locations.size() > 0) {
            for (int i = 0; i < (Float) params.get(DENSITY); i++) {
                int randomIndex = (int) MathUtils.randomInRange(0, locations.size());
                if (!usedIndexes.contains(randomIndex)) {
                    usedIndexes.add(randomIndex);
                    gameObjects.add(placeObstacle(locations.get(randomIndex), offset));
                }
            }
        }

        return gameObjects;
    }

    public List<GameObject> generateAt(List<Vector3> locations, Map<String, Object> params, Vector3 offset) {
        List<GameObject> gameObjects = new ArrayList<GameObject>();

        for (Vector3 loc : locations) {
            gameObjects.add(placeObstacle(loc, offset));
        }

        return gameObjects;
    }


    abstract GameObject placeObstacle(Vector3 vector, Vector3 offset);

}
