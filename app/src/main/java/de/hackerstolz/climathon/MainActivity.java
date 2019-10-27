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
package de.hackerstolz.climathon;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.hackerstolz.climathon.WebViewController;

/**
* This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
*/

public class MainActivity extends AppCompatActivity {
	public class SceneNotValid extends RuntimeException{};
	class FuckedUpInterpolation extends TimerTask
	{
		long start, end, length;
		public FuckedUpInterpolation(long length) {
			this.length = length;
		}

		@Override
		public void run() {
			long current = new Date().getTime();
			if (start == 0) {
				start = current;
				end = start + length;
			}
			setFucked((current - start) / (float)(end - start));
			if (current > end) {
				Log.w(TAG, "finished setFucked Timer");
				this.cancel();

				MainActivity.this.runOnUiThread(() -> {
					webView.animate().alpha(1.0f).setDuration(400).setListener(null);
				});
			}
		}
	}

	public static final String TAG = MainActivity.class.getSimpleName();
	private static final double MIN_OPENGL_VERSION = 3.0;

	private WebView webView;
	
	private HorizontalArFragment arFragment;
	private AnchorNode worldAnchorNode;

	private ModelRenderable earthRenderable;
	private ModelRenderable colaRenderable;
	private ModelRenderable lipstickRenderable;
	private ModelRenderable cupRenderable;
	private ModelRenderable garbageRenderable;
	private ModelRenderable sneakersRenderable;
	private ModelRenderable waterRenderable;
	
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
		setupWebView();
		setupAR();
	}

	private void setupWebView() {
		webView = (WebView)findViewById(R.id.webview);
		webView.setBackgroundColor(Color.TRANSPARENT);
		webView.setWebViewClient(new WebViewController(this));
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setAlpha(0.f);
		navigateTo("start");
	}

	private void setupAR() {
		arFragment = (HorizontalArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

		ModelRenderable.builder()
			.setSource(this, R.raw.earth)
			.build()
			.thenAccept(renderable -> earthRenderable = renderable)
			.thenRun(() -> setFucked(0.f))
			.thenRun(() -> spawnAREarth());

		ModelRenderable.builder()
				.setSource(this, R.raw.cola)
				.build()
				.thenAccept(renderable -> colaRenderable = renderable);
		ModelRenderable.builder()
				.setSource(this, R.raw.lipstick)
				.build()
				.thenAccept(renderable -> lipstickRenderable = renderable);
		ModelRenderable.builder()
				.setSource(this, R.raw.cup)
				.build()
				.thenAccept(renderable -> cupRenderable = renderable);

		ModelRenderable.builder()
				.setSource(this, R.raw.garbage)
				.build()
				.thenAccept(renderable -> garbageRenderable = renderable);
		ModelRenderable.builder()
				.setSource(this, R.raw.sneakers)
				.build()
				.thenAccept(renderable -> sneakersRenderable = renderable);
		ModelRenderable.builder()
				.setSource(this, R.raw.model)
				.build()
				.thenAccept(renderable -> waterRenderable = renderable);
	}

	private void navigateTo(String path) {
		webView.loadUrl("http://escape-apocalypse.s3-website.eu-central-1.amazonaws.com/" + path);
	}
	
	private Point getScreenCenter() {
		if(arFragment == null || arFragment.getView() == null) {
			return new android.graphics.Point(0,0);
		}
		
		int w = arFragment.getView().getWidth()/2;
		int h = arFragment.getView().getHeight()/2;
		return new android.graphics.Point(w, h);
	}

	public void setFucked(float fucked) {
		Log.d(TAG, "set fucked: " + fucked);
		earthRenderable.getMaterial().setFloat("fucked", Math.min(fucked, 1.f));
	}

	private AnchorNode anchorNodeForScreenCenter() {
		ArSceneView sceneView = arFragment.getArSceneView();
		Frame frame = sceneView.getArFrame();
		if (frame == null) {
			Log.w(TAG, "frame null");
			return null;
		}

		Point center = getScreenCenter();
		List<HitResult> hitTest = frame.hitTest(center.x, center.y);
		if (hitTest.size() <= 0) {
			Log.w(TAG, "hit result null");
			return null;
		}
		HitResult hitTestResult = hitTest.get(0);

		Log.w(TAG, "spawning earth");
		// Create the Sceneform AnchorNode
		Anchor anchor = hitTestResult.createAnchor();
		return new AnchorNode(anchor);
	}

	private void placeItemInLine(AnchorNode anchorNode, Renderable renderable, float leftOffset, float scale) {
		Camera cam = arFragment.getArSceneView().getScene().getCamera();
		Node item = new Node();
		item.setParent(anchorNode);
		item.setRenderable(renderable);
		item.setLocalScale(Vector3.one().scaled(scale));
		item.setLocalPosition(cam.getRight().scaled(leftOffset));
	}

	public void plasticScene() {
		try {
		AnchorNode anchorNode = anchorNodeForScreenCenter();
		if (anchorNode == null) {
			throw new SceneNotValid();
		}
		anchorNode.setParent(arFragment.getArSceneView().getScene());

		// Water
		placeItemInLine(anchorNode, waterRenderable, 0.f, 0.4f);

		placeItemInLine(anchorNode, lipstickRenderable, 1.f, 1f);
		placeItemInLine(anchorNode, cupRenderable, 1.28f, 0.14f);
		placeItemInLine(anchorNode, sneakersRenderable, 1.6f, 0.21f);

		placeItemInLine(anchorNode, colaRenderable, 2.6f, 0.2f);

		// Cola
		} catch (Exception e) {
			final Handler handler = new Handler();
			handler.postDelayed(() -> plasticScene(), 500);
			Log.w(TAG, "rerunning plasticScene in 500");
		}
	}
	
	public void spawnAREarth() {
		try {
			if (arFragment.getArSceneView().getArFrame().getCamera().getTrackingState() != TrackingState.TRACKING) {
				Log.w(TAG, "not tracking");
				throw new SceneNotValid();
			}

			worldAnchorNode = anchorNodeForScreenCenter();
			if (worldAnchorNode == null) {
				throw new SceneNotValid();
			}
			worldAnchorNode.setParent(arFragment.getArSceneView().getScene());
			
			// Create the node relative to the AnchorNode
			EarthNode earthNode = new EarthNode();
			earthNode.setParent(worldAnchorNode);
			
			// Create the transformable andy and add it to the anchor.
			TransformableNode earth = new TransformableNode(arFragment.getTransformationSystem());
			earth.setParent(earthNode);
			earth.setRenderable(earthRenderable);
			new Timer().scheduleAtFixedRate(new FuckedUpInterpolation(10000), 2000, 50);
		} catch (Exception e) {
			final Handler handler = new Handler();
			handler.postDelayed(() -> spawnAREarth(), 500);
			Log.w(TAG, "rerunning spawnAREarth in 500");
		}
	}

	public void spawnScreenSpaceEarth() {
		Camera camera = arFragment.getArSceneView().getScene().getCamera();

		arFragment.getArSceneView().getScene().removeChild(worldAnchorNode);
		Node node = new Node();
		node.setParent(camera);
		node.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
		Vector3 pos = new Vector3(-0.15f, 0.3f ,-0.7f);
		node.setLocalPosition(pos);
		node.setRenderable(earthRenderable);
		earthRenderable.setShadowCaster(false);
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
