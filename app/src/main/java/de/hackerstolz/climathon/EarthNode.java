package de.hackerstolz.climathon;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

public class EarthNode extends Node {
    @Override
    public void onUpdate(FrameTime frameTime) {
        setLocalRotation(Quaternion.axisAngle(Vector3.up(), (10.f * frameTime.getStartSeconds()) % 360));
    }

}
