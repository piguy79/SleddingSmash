package com.railwaygames.sleddingsmash.levels.obstacles;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.railwaygames.sleddingsmash.entity.GameObject;

/**
 * Created by conormullen on 2/19/15.
 */
public class TreeObstacleGenerator extends ObstacleGenerator {

    public TreeObstacleGenerator(Model model) {
        super(model);
    }

    @Override
    GameObject placeObstacle(Vector3 vector, Vector3 offset) {

        btCollisionShape colliosionShape =  Bullet.obtainStaticNodeShape(model.nodes);
        GameObject.Constructor constructor = new GameObject.Constructor(model, GameObject.GameObjectType.TREE, colliosionShape, 0);
        GameObject tree = constructor.construct();

        tree.setPosition(vector);
        Vector3 position = new Vector3(offset.x + vector.x, vector.y + (tree.height() / 2), vector.z);
        tree.transform.setToTranslation(position);
        tree.getBody().setWorldTransform(tree.transform);

        return tree;
    }
}
