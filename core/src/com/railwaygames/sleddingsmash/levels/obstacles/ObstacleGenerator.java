package com.railwaygames.sleddingsmash.levels.obstacles;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.railwaygames.sleddingsmash.entity.GameObject;
import com.railwaygames.sleddingsmash.utils.MathUtils;
import com.railwaygames.sleddingsmash.utils.ModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by conormullen on 2/19/15.
 */
public abstract class ObstacleGenerator{

    Model model;

    protected ObstacleGenerator(Model model) {
        this.model = model;
    }

    public List<GameObject> generateObstacles(Model areaModel, int density, ModelUtils.RectangleArea area, Vector3 upVector){
        List<GameObject> gameObjects = new ArrayList<GameObject>();
        List<Vector3> locations = ModelUtils.findAreaInModel(areaModel, area, upVector);

        List<Integer> usedIndexes = new ArrayList<Integer>();

        for(int i = 0; i < density; i++){
            int randomIndex = (int) MathUtils.randomInRange(0, locations.size());
            if(!usedIndexes.contains(randomIndex)){
                usedIndexes.add(randomIndex);
                gameObjects.add(placeObstacle(locations.get(randomIndex)));
            }
        }


        return gameObjects;
    }

    abstract GameObject placeObstacle(Vector3 vector);

}
