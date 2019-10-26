/*
* Copyright 2018 Google LLC. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.google.ar.sceneform.samples.hellosceneform;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.List;

/**
* This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
*/

public class MainActivity extends AppCompatActivity {
	public class SceneNotValid extends RuntimeException{};

	private static final String TAG = MainActivity.class.getSimpleName();
	private static final double MIN_OPENGL_VERSION = 3.0;
	private static boolean placedEarth = false;
	
	private ArFragment arFragment;
	private ModelRenderable earthRenderable;
	private ModelRenderable andyRenderable;
	
	@Override
	@SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
	// CompletableFuture requires api level 24
	// FutureReturnValueIgnored is not valid
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!checkIsSupportedDeviceOrFinish(this)) {
			return;
		}
		
		setContentView(R.layout.activity_ux);
		arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
		
		// When you build a Renderable, Sceneform loads its resources in the background while returning
		// a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
		ModelRenderable.builder()
			.setSource(this, R.raw.earth)
			.build()
			.thenAccept(renderable -> earthRenderable = renderable)
			.thenRun(() -> spawnAREarth());

		arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
			if (andyRenderable == null || hitResult == null) {
				return;
			}

			Log.w(TAG, "spawning andy");

			// Create the Anchor.
			Anchor anchor = hitResult.createAnchor();
			AnchorNode anchorNode = new AnchorNode(anchor);
			anchorNode.setParent(arFragment.getArSceneView().getScene());

			// Create the transformable andy and add it to the anchor.
			TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
			andy.setParent(anchorNode);
			andy.setRenderable(andyRenderable);
			andy.select();
		});
	}
	
	private Point getScreenCenter() {
		if(arFragment == null || arFragment.getView() == null) {
			return new android.graphics.Point(0,0);
		}
		
		int w = arFragment.getView().getWidth()/2;
		int h = arFragment.getView().getHeight()/2;
		return new android.graphics.Point(w, h);
	}
	
	public void spawnAREarth() {
		try {
			if (arFragment.getArSceneView().getArFrame().getCamera().getTrackingState() != TrackingState.TRACKING) {
				Log.w(TAG, "not tracking");
				throw new SceneNotValid();
			}
			ArSceneView sceneView = arFragment.getArSceneView();
			Frame frame = sceneView.getArFrame();
			if (frame == null) {
				Log.w(TAG, "frame null");
				throw new SceneNotValid();
			}

			Point center = getScreenCenter();
			List<HitResult> hitTest = frame.hitTest(center.x, center.y);
			if (hitTest.size() <= 0) {
				Log.w(TAG, "hit result null");
				throw new SceneNotValid();
			}
			HitResult hitTestResult = hitTest.get(0);

			Log.w(TAG, "spawning earth");
			// Create the Sceneform AnchorNode
			Anchor anchor = hitTestResult.createAnchor();
			AnchorNode anchorNode = new AnchorNode(anchor);
			anchorNode.setParent(arFragment.getArSceneView().getScene());
			
			// Create the node relative to the AnchorNode
			EarthNode earthNode = new EarthNode();
			earthNode.setParent(anchorNode);
			earthNode.setLocalPosition(Vector3.up().scaled(0.8f));
			
			// Create the transformable andy and add it to the anchor.
			TransformableNode earth = new TransformableNode(arFragment.getTransformationSystem());
			earth.setParent(earthNode);
			earth.setRenderable(earthRenderable);
			earth.select();
			MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
				.thenAccept(material -> {
					earthRenderable.setMaterial(material);
				}
			);

			placedEarth = true;
		} catch (Exception e) {
			final Handler handler = new Handler();
			handler.postDelayed(() -> spawnAREarth(), 500);
			Log.w(TAG, "rerunning spawnAREarth in 500");
		}
	}

	public void makeEarthScreenSpace() {
		Node node = new Node();
		node.setParent(arFragment.getArSceneView().getScene());
		node.setRenderable(earthRenderable);
	}
	
	/**
	* Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
	* on this device.
	*
	* <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
	*
	* <p>Finishes the activity if Sceneform can not run
	*/
	public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
		if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
			Log.e(TAG, "Sceneform requires Android N or later");
			Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
			activity.finish();
			return false;
		}
		String openGlVersionString =
		((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
		.getDeviceConfigurationInfo()
		.getGlEsVersion();
		if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
			Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
			Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
			.show();
			activity.finish();
			return false;
		}
		return true;
	}
}
