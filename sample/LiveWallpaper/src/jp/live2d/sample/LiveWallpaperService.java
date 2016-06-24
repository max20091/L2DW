/**
 *
 *  You can modify and use this source freely
 *  only for the development of application related Live2D.
 *
 *  (c) Live2D Inc. All rights reserved.
 */
package jp.live2d.sample;

import android.view.MotionEvent;

import net.rbgrn.android.glwallpaperservice.GLWallpaperService;

import jp.live2d.utils.android.FileManager;

public class LiveWallpaperService extends GLWallpaperService  {

	
	public LiveWallpaperService() {
		super();
	}


	public Engine onCreateEngine() {
		MyEngine engine = new MyEngine();
		return engine;
	}

	class MyEngine extends GLEngine {

		private Live2DRenderer 				renderer ;
	
		public MyEngine() {
			super();
			renderer = new Live2DRenderer(getApplicationContext());
			setRenderer(renderer);
			setRenderMode(RENDERMODE_CONTINUOUSLY);
			renderer.startAccel(getApplicationContext());
			FileManager.init(getApplicationContext());
		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
					renderer.touchesBegan(event.getX(),event.getY());
					
				break;
			case MotionEvent.ACTION_UP:
				renderer.touchesEnded();
				
				break;
			case MotionEvent.ACTION_MOVE:
					renderer.touchesMoved(event.getX(),event.getY());
				break;
			case MotionEvent.ACTION_CANCEL:
				break;
			}
			 renderer.gestureDetector.onTouchEvent(event) ;
		}

		@Override
        public void onVisibilityChanged(boolean visible) {
            if (!visible) {
            	renderer.motionMgr.stopAllMotions();
            	renderer.onPause();
            } else {
            	renderer.motionMgr.startMotion(renderer.motion, false);
            	renderer.onResume();
            
            }
        }
		@Override
	    public void onOffsetsChanged(float xOffset, float yOffset,
	            float xStep, float yStep, int xPixels, int yPixels) {
			//you can set a motion/reaction of the model here (mtn / expression)
			//when screen flicked in general (switch home screens)
	    } 
		

		public void onDestroy() {
			super.onDestroy();
			if (renderer != null) {
				renderer.release();
			}
			renderer = null;
		}
	}
}