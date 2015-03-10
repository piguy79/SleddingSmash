package com.railwaygames.sleddingsmash.levels.obstacles;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;
import com.badlogic.gdx.physics.bullet.collision.btConeShape;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape;
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

        btCylinderShape cylinder = new btCylinderShape(new Vector3(2,6,2));
        GameObject.Constructor constructor = new GameObject.Constructor(model, GameObject.GameObjectType.TREE, cylinder, 0);
        GameObject tree = constructor.construct();

        tree.setPosition(vector);
        Vector3 position = new Vector3(offset.x + vector.x, vector.y + (tree.height() / 2), vector.z);
        tree.transform.setToTranslation(position);
        Matrix4 treePosition = tree.transform.cpy();
        treePosition.translate(4, -1, -1);
        tree.getBody().setWorldTransform(treePosition);

        return tree;
    }
}
