package com.railwaygames.sleddingsmash.levels.obstacles;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape;
import com.railwaygames.sleddingsmash.Constants;
import com.railwaygames.sleddingsmash.entity.GameObject;
import com.railwaygames.sleddingsmash.utils.MathUtils;

import java.util.Map;

/**
 * Created by conormullen on 2/19/15.
 */
public class TreeObstacleGenerator extends ObstacleGenerator {

    public TreeObstacleGenerator(Model model) {
        super(model);
    }

    @Override
    GameObject placeObstacle(Vector3 vector, Vector3 offset, Map<String, Object> params) {

        btCylinderShape cylinder = new btCylinderShape(new Vector3(2, 6, 2));
        GameObject.Constructor constructor = new GameObject.Constructor(model, GameObject.GameObjectType.TREE, cylinder, 0);
        GameObject tree = constructor.construct();

        tree.setPosition(vector);
        Vector3 position = new Vector3(offset.x + vector.x, vector.y + (tree.height() / 2), vector.z);
        tree.transform.setToTranslation(position);
        Matrix4 treePosition = tree.transform.cpy();

        float round = MathUtils.randomInRange(1.0f, 2.5f);
        tree.transform.scale(round, MathUtils.randomInRange(1.2f, 3.5f), round);

        treePosition.translate(4, -1, -1);
        tree.getBody().setWorldTransform(treePosition);
        tree.getBody().setContactCallbackFlag(Constants.CollisionsFlag.TREE_FLAG);
        tree.getBody().setContactCallbackFilter(Constants.CollisionsFlag.SPHERE_FLAG);

        return tree;
    }
}
