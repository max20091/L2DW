/**
 *
 *  You can modify and use this source freely
 *  only for the development of application related Live2D.
 *
 *  (c) Live2D Inc. All rights reserved.
 */
package jp.live2d.sample;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import net.rbgrn.android.glwallpaperservice.GLWallpaperService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jp.live2d.android.Live2DModelAndroid;
import jp.live2d.android.UtOpenGL;
import jp.live2d.framework.L2DEyeBlink;
import jp.live2d.framework.L2DMatrix44;
import jp.live2d.framework.L2DModelMatrix;
import jp.live2d.framework.L2DPhysics;
import jp.live2d.framework.L2DStandardID;
import jp.live2d.framework.L2DTargetPoint;
import jp.live2d.framework.L2DViewMatrix;
import jp.live2d.motion.Live2DMotion;
import jp.live2d.motion.MotionQueueManager;
import jp.live2d.util.UtSystem;
import jp.live2d.utils.android.AccelHelper;
import jp.live2d.utils.android.BufferUtil;
import jp.live2d.utils.android.FileManager;
import jp.live2d.utils.android.ModelSetting;
import jp.live2d.utils.android.ModelSettingJson;
import jp.live2d.utils.android.OffscreenImage;
import jp.live2d.utils.android.SimpleImage;
import jp.live2d.utils.android.TouchManager;


public class Live2DRenderer implements GLWallpaperService.Renderer
{
	Context con;
	TouchManager touchMgr;
	Live2DModelAndroid	live2DModel ;
	Live2DMotion motion;
	MotionQueueManager motionMgr;
	private L2DTargetPoint dragMgr;
	L2DPhysics physics;
	
	
	protected float 				accelX=0;
	protected float 				accelY=0;
	protected float 				accelZ=0;
	protected float 				dragX=0;
	protected float 				dragY=0;
	protected boolean 				initialized = false;	
	protected boolean 				updating = false;

	/// Don't move this to LAppDefine!
	final String TEXTURE_PATHS[] =
		{
				"Asuha/Asuha_model/texture_00.png",
				"Asuha/Asuha_model/texture_01.png"
		} ;
	final String MOTION_PATH="Asuha/motions/Asuha_Idle.mtn"; //Idle motion

	final String PHYSICS_PATH="Asuha/physics.json"; //Physics file

	float glWidth=0;
	float glHeight=0;

	public SimpleImage bg; // BackGround Image
	
	public float modelWidth = 0;
	private float aspect = 0;
	protected float 				alpha = 1;	
	
	static public final String 	TAG = "SampleLive2DManager";
	private ModelSetting modelSetting = null;
	static FloatBuffer 				debugBufferVer = null ;
	static FloatBuffer 				debugBufferColor = null ;
	protected L2DModelMatrix modelMatrix=null;
	
	private L2DMatrix44 deviceToScreen;
	private L2DViewMatrix viewMatrix;
	private AccelHelper accelHelper;
	GestureDetector 					gestureDetector;
	protected long 					startTimeMSec;
	protected L2DEyeBlink eyeBlink;
	
	//My custom booleans needed for my model to work
	public boolean isNormal=true;
	public boolean isHappy=false;
	public boolean isAngry=false;
	Live2DMotion motionAngry;
	Live2DMotion motionHappy;
	Live2DMotion motionNormal;
	////////////////////////////////////////////////////////////

	public Live2DRenderer(Context context)
	{
		
		con = context;
		motionMgr=new MotionQueueManager();
		dragMgr=new L2DTargetPoint();
		eyeBlink=new L2DEyeBlink();
		touchMgr=new TouchManager();
		deviceToScreen=new L2DMatrix44();
		gestureDetector = new GestureDetector(con , simpleOnGestureListener ) ;
		
		viewMatrix=new L2DViewMatrix();
		
		
		viewMatrix.setMaxScreenRect(
				LAppDefine.VIEW_LOGICAL_MAX_LEFT,
				LAppDefine.VIEW_LOGICAL_MAX_RIGHT,
				LAppDefine.VIEW_LOGICAL_MAX_BOTTOM,
				LAppDefine.VIEW_LOGICAL_MAX_TOP
				);

	}
	


	public void onDrawFrame(GL10 gl) {
        // Your rendering code goes here
		modelWidth = live2DModel.getCanvasWidth();
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		update();

		gl.glMatrixMode(GL10.GL_MODELVIEW ) ;
		gl.glLoadIdentity() ;

		float SCALE_X = 0.25f ;
		float SCALE_Y = 0.1f ;
		gl.glTranslatef( -SCALE_X  * accelX , SCALE_Y * accelY , 0 ) ;

		
		bg.draw(gl);	// background image draw 
		// Add drawHitArea(gl) line if you need to draw hit areas to the screen

		// Live2D model adjust
		gl.glScalef(2.0f, 2.0f, 2.0f); // scale(x, y, z)
		gl.glTranslatef(0.0f, -0.3f, 0.0f);	// position(x, y, z)
		gl.glOrthof(0 ,	modelWidth , modelWidth / aspect , 0 , 0.5f , -0.5f ) ;
		
		gl.glPopMatrix() ;
		

		live2DModel.loadParam();

		if(motionMgr.isFinished())
		{
			//If you have only 1 motion
			//motionMgr.startMotion(motion, false);
			if (isNormal){
				motionMgr.startMotion(motion, false);
			}
			else if (isHappy){
				isHappy = false;
				motionMgr.startMotion(motion, false);
			}
			else if (isAngry){
				isAngry = false;
				motionMgr.startMotion(motion, false);
			} else {
				isNormal = true;
			}
		}
		else
		{
			motionMgr.updateParam(live2DModel);
		}
		boolean update = motionMgr.updateParam(live2DModel);
		if( ! update)
		{
			eyeBlink.updateParam(live2DModel);
		}
		live2DModel.saveParam();

		dragMgr.update();

		float dragX=dragMgr.getX();
		float dragY=dragMgr.getY();
		
		long timeMSec = UtSystem.getUserTimeMSec() - startTimeMSec  ;
		double timeSec = timeMSec / 1000.0 ;
		double t = timeSec * 2 * Math.PI  ;

		live2DModel.addToParamFloat( L2DStandardID.PARAM_ANGLE_X, dragX *  30 , 1 );
		
		live2DModel.addToParamFloat( L2DStandardID.PARAM_ANGLE_Y, dragY *  30 , 1 );
		
		live2DModel.addToParamFloat( L2DStandardID.PARAM_BODY_ANGLE_X , dragX * 10 , 1 );
		
		live2DModel.addToParamFloat( L2DStandardID.PARAM_EYE_BALL_X, dragX*1 , 1 );
		live2DModel.addToParamFloat( L2DStandardID.PARAM_EYE_BALL_Y, dragY*1 , 1 );
		
		live2DModel.addToParamFloat(L2DStandardID.PARAM_ANGLE_X,	(float) (15 * Math.sin( t/ 6.5345 )) , 0.5f);
		live2DModel.addToParamFloat(L2DStandardID.PARAM_ANGLE_Y,	(float) ( 8 * Math.sin( t/ 3.5345 )) , 0.5f);
	
		live2DModel.addToParamFloat(L2DStandardID.PARAM_BODY_ANGLE_X,(float) ( 4 * Math.sin( t/15.5345 )) , 0.5f);
		live2DModel.setParamFloat(L2DStandardID.PARAM_BREATH,	(float) (0.5f + 0.5f * Math.sin( t/3.2345 )),1);
		
		//////THIS IS FOR ACCEL EFFECT WHEN TILTING THE DEVICE, in my case it's "X" some others maybe "Z"
		live2DModel.addToParamFloat(L2DStandardID.PARAM_BODY_ANGLE_X, 60 * accelX  ,0.5f);
		live2DModel.addToParamFloat(L2DStandardID.PARAM_ANGLE_X, 60 * accelX  ,0.5f);
		////////////////////////////////////////////
	
		if(physics!=null)physics.updateParam(live2DModel);

		live2DModel.setGL( gl ) ;
		live2DModel.update();
		live2DModel.draw() ;
	
		/////////////////////////////////////
    }
	
    public void onSurfaceChanged(GL10 gl, int width, int height) {
    	
    	setupView(width,height);
		gl.glViewport( 0 , 0 , width , height ) ;
		gl.glMatrixMode( GL10.GL_PROJECTION ) ;
		gl.glLoadIdentity() ; 
		modelWidth = live2DModel.getCanvasWidth(); 
		aspect = (float)width/height;
		gl.glOrthof(-2.0f ,	2.0f , -2.0f ,2.0f , 0.5f , -0.5f ) ;
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		glWidth=width;
		glHeight=height; 
		OffscreenImage.createFrameBuffer(gl, width ,height, 0);
		return ;
	    
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
    	AssetManager mngr = con.getAssets();
    	//GET MOC FILE
		try
		{
			InputStream in = mngr.open( LAppDefine.MODEL_ASUHA_MOC ) ;
			live2DModel = Live2DModelAndroid.loadModel( in ) ;
			in.close() ;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		//SETTING JSON NEEDED FOR DRAWING HIT AREA///////////
    	try
		{
			InputStream in = mngr.open( LAppDefine.MODEL_ASUHA ) ;
			modelSetting = new ModelSettingJson(in);
			modelMatrix=new L2DModelMatrix(live2DModel.getCanvasWidth(),live2DModel.getCanvasHeight());
			modelMatrix.setWidth(2);
			modelMatrix.setCenterPosition(0, 0);
			in.close() ;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
    	////////////////////////////////////////////////////////////////////////////////
		try
		{
						setupBackground(gl); /////SETUP THE BACKGROUND

			for (int i = 0 ; i < TEXTURE_PATHS.length ; i++ )
			{
				InputStream in = mngr.open( TEXTURE_PATHS[i] ) ;
				int texNo = UtOpenGL.loadTexture(gl , in , true ) ;
				live2DModel.setTexture( i , texNo ) ;
				in.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		try
		{
			InputStream in = mngr.open( MOTION_PATH ) ;
			motion = Live2DMotion.loadMotion( in ) ;
			in.close() ;

			in=mngr.open(PHYSICS_PATH);
			physics=L2DPhysics.load(in);
			in.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		//myCODE needed for my motions END///////////////
		try
		{
			InputStream in = mngr.open( LAppDefine.MOTION_PATH_ANGRY ) ;
			motionAngry = Live2DMotion.loadMotion( in ) ;
			in.close() ;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			InputStream in = mngr.open( LAppDefine.MOTION_PATH_HAPPY ) ;
			motionHappy = Live2DMotion.loadMotion( in ) ;
			in.close() ;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			InputStream in = mngr.open( LAppDefine.MOTION_PATH_NORMAL ) ;
			motionNormal = Live2DMotion.loadMotion( in ) ;
			in.close() ;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		//myCODE needed for my motions END///////////////
    }

  
    
   

    public void resetDrag()
    {
    	dragMgr.set(0, 0);
    }
   
    public void setupView( int width, int height)
	{
		float ratio=(float)height/width;
		float left = LAppDefine.VIEW_LOGICAL_LEFT;
		float right = LAppDefine.VIEW_LOGICAL_RIGHT;
		float bottom = -ratio;
		float top = ratio;

		viewMatrix.setScreenRect(left,right,bottom,top);

		float screenW=Math.abs(left-right);
		deviceToScreen.identity() ;
		deviceToScreen.multTranslate(-width/2.0f,height/2.0f );
		deviceToScreen.multScale( screenW/width , screenW/width );
	}
    
    public void startAccel(Context context)
	{
		
		accelHelper = new AccelHelper(context) ;
	}
    
    public void setAccel(float x,float y,float z)
	{
		accelX=x;
		accelY=y;
		accelZ=z;
	}


	public void setDrag(float x,float y)
	{
		dragX=x;
		dragY=y;
	}
	
	public void update()
	{		
		dragMgr.update();
		setDrag(dragMgr.getX(), dragMgr.getY());

		accelHelper.update();

		if( accelHelper.getShake() > 1.5f )
		{
			if(LAppDefine.DEBUG_LOG)Log.d(TAG, "shake event");
			
			shakeEvent() ;
			accelHelper.resetShake() ;
		}

		setAccel(accelHelper.getAccelX(), accelHelper.getAccelY(), accelHelper.getAccelZ()); 
	}
    //MYCODE
    
    /////////////////////////////////////////////////////////////////////////
    
    public L2DModelMatrix getModelMatrix()
	{
		return modelMatrix;
	}
    
    private void drawHitArea(GL10 gl) {
		gl.glDisable( GL10.GL_TEXTURE_2D ) ;
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY) ;
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glPushMatrix() ;
		{
			gl.glMultMatrixf(modelMatrix.getArray(), 0) ;
			int len = modelSetting.getHitAreasNum();
			for (int i=0;i<len;i++)
			{
				String drawID=modelSetting.getHitAreaID(i);
				int drawIndex=live2DModel.getDrawDataIndex(drawID);
				if(drawIndex<0)continue;
				float[] points=live2DModel.getTransformedPoints(drawIndex);
				float left=live2DModel.getCanvasWidth();
				float right=0;
				float top=live2DModel.getCanvasHeight();
				float bottom=0;

				for (int j = 0; j < points.length; j=j+2)
				{
					float x = points[j];
					float y = points[j+1];
					if(x<left)left=x;	
					if(x>right)right=x;	
					if(y<top)top=y;		
					if(y>bottom)bottom=y;
				}

				float[] vertex={left,top,right,top,right,bottom,left,bottom,left,top};
				float r=1;
				float g=0;
				float b=0;
				float a=0.5f;
				int size=5;
				float color[] = {r,g,b,a,r,g,b,a,r,g,b,a,r,g,b,a,r,g,b,a};


				gl.glLineWidth( size );	
				gl.glVertexPointer( 2, GL10.GL_FLOAT, 0, BufferUtil.setupFloatBuffer( debugBufferVer,vertex));	
				gl.glColorPointer( 4, GL10.GL_FLOAT, 0, BufferUtil.setupFloatBuffer( debugBufferColor,color ) );	
		    	gl.glDrawArrays( GL10.GL_LINE_STRIP, 0, 5 );	
			}
		}
		gl.glPopMatrix() ;
		gl.glEnable( GL10.GL_TEXTURE_2D ) ;
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY) ;
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
	}
    
    public boolean hitTest(String id,float testX,float testY)
	{
		if(alpha<1)return false;
		if(modelSetting==null)return false;
		int len=modelSetting.getHitAreasNum();
		for (int i = 0; i < len; i++)
		{
			if( id.equals(modelSetting.getHitAreaName(i)) )
			{
				return hitTestSimple(modelSetting.getHitAreaID(i),testX,testY) ;
			}
		}
		return false;
	}
    
    public boolean hitTestSimple(String drawID,float testX,float testY)
	{
		if(alpha<1)return false;

		int drawIndex=live2DModel.getDrawDataIndex(drawID);
		if(drawIndex<0)return false;
		float[] points=live2DModel.getTransformedPoints(drawIndex);

		float left=live2DModel.getCanvasWidth();
		float right=0;
		float top=live2DModel.getCanvasHeight();
		float bottom=0;

		for (int j = 0; j < points.length; j=j+2)
		{
			float x = points[j];
			float y = points[j+1];
			if(x<left)left=x;	
			if(x>right)right=x;	
			if(y<top)top=y;		
			if(y>bottom)bottom=y;
		}

		float tx=modelMatrix.invertTransformX(testX);
		float ty=modelMatrix.invertTransformY(testY);

		return ( left <= tx && tx <= right && top <= ty && ty <= bottom ) ;
	}
    public void shakeEvent()
	{
    	motionMgr.startMotion(motionNormal, false);
	}
  
    
    public boolean tapEvent(float x,float y) 
	{

			if(hitTest(  LAppDefine.HIT_AREA_HEAD,x, y ))
			{
				//if(LAppDefine.DEBUG_LOG)Log.d(TAG, "Tap face.");
				motionMgr.startMotion(motionHappy, false);
				isHappy = true;
				isNormal = false;
				isAngry = false;
				
			}
			//Need to detect boobs hitbox first because it is smaller than body hitbox
			else if(hitTest( LAppDefine.HIT_AREA_BREAST,x, y))
			{
				//if(LAppDefine.DEBUG_LOG)Log.d(TAG, "Tap boobs.");
				motionMgr.startMotion(motionAngry, false);
				isAngry = true;
				isHappy = false;
				isNormal = false;
			}
			else if(hitTest( LAppDefine.HIT_AREA_BODY,x, y))
			{
				//if(LAppDefine.DEBUG_LOG)Log.d(TAG, "Tap body.");
				motionMgr.startMotion(motionNormal, false);
				isNormal = true;
				isHappy = false;
				isAngry = false;
			}
		return true;
	}
    
    public void flickEvent(float x,float y)
	{

			if(hitTest( LAppDefine.HIT_AREA_HEAD, x, y ))
			{
				//if(LAppDefine.DEBUG_LOG)Log.d(TAG, "Flick head.");
				motionMgr.startMotion(motionHappy, false);
				isHappy = true;
				isNormal = false;
				isAngry = false;
			}
		
	}
    
    
    private float transformDeviceToViewX(float deviceX)
	{
		float screenX = deviceToScreen.transformX( deviceX );
		return  viewMatrix.invertTransformX(screenX);
	}


	private float transformDeviceToViewY(float deviceY)
	{
		float screenY = deviceToScreen.transformY( deviceY );
		return  viewMatrix.invertTransformY(screenY);
	}
	
	public void touchesBegan(float p1x,float p1y)
	{
		if(LAppDefine.DEBUG_TOUCH_LOG)Log.v(TAG, "touchesBegan"+" x:"+p1x+" y:"+p1y);
		touchMgr.touchBegan(p1x,p1y);

		float x=transformDeviceToViewX( touchMgr.getX() );
		float y=transformDeviceToViewY( touchMgr.getY() );

		dragMgr.set(x, y);
	}

	public void touchesMoved(float p1x,float p1y)
	{
		if(LAppDefine.DEBUG_TOUCH_LOG)Log.v(TAG, "touchesMoved"+"x:"+p1x+" y:"+p1y);
		touchMgr.touchesMoved(p1x,p1y);
		float x=transformDeviceToViewX( touchMgr.getX() );
		float y=transformDeviceToViewY( touchMgr.getY() );
		//float x=p1x/glWidth*2-1;  //from original sample wallpaper, not much difference
		//float y=-p1y/glHeight*2+1; //from original sample wallpaper, not much difference
		dragMgr.set(x, y);

		
		final int FLICK_DISTANCE=100;

		if(touchMgr.isSingleTouch() && touchMgr.isFlickAvailable() )
		{
			float flickDist=touchMgr.getFlickDistance();
			if(flickDist>FLICK_DISTANCE)
			{

				float startX=transformDeviceToViewX( touchMgr.getStartX() );
				float startY=transformDeviceToViewY( touchMgr.getStartY() );
				flickEvent(startX,startY);
				touchMgr.disableFlick();
			}
		}
	}

	public void touchesEnded()
	{
		if(LAppDefine.DEBUG_TOUCH_LOG)Log.v(TAG, "touchesEnded");
		dragMgr.set(0,0);
	}
	
	private final SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener()
	{
        @Override
        public boolean onDoubleTap(MotionEvent event)
        {
        	return super.onDoubleTap(event) ;
        }

        @Override
        public boolean onDown(MotionEvent event)
        {
            super.onDown(event);
            return true ;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event)
        {
        	float x=transformDeviceToViewX( touchMgr.getX() );
    		float y=transformDeviceToViewY( touchMgr.getY() );
          	boolean ret = tapEvent(x,y); //Live2D Event
          	ret |= super.onSingleTapUp(event);
            return ret ;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event)
        {
            return super.onSingleTapUp(event) ;
        }
    };
    
    /**
     * Called when the engine is destroyed. Do any necessary clean up because
     * at this point your renderer instance is now done for.
     */
    public void release() {
    	live2DModel.deleteTextures();
    }
    
	public void onResume()
	{
		if(accelHelper!=null)
		{
			if(LAppDefine.DEBUG_LOG)Log.d(TAG, "start accelHelper");
			accelHelper.start();
		}
	}


	
	public void onPause()
	{
		if(accelHelper!=null)
		{
			if(LAppDefine.DEBUG_LOG)Log.d(TAG, "stop accelHelper");
			accelHelper.stop();
		}
	}
	/*
	 * BackGround Image Setting
	 */
	private void setupBackground(GL10 context) {
		try {
			InputStream in = FileManager.open(LAppDefine.BACK_IMAGE_NAME);
			bg=new SimpleImage(context,in);
			//Adjust the values bellow for background position from LAppDefine
			bg.setDrawRect(
					LAppDefine.VIEW_LOGICAL_MAX_LEFT,
					LAppDefine.VIEW_LOGICAL_MAX_RIGHT,
					LAppDefine.VIEW_LOGICAL_MAX_BOTTOM,
					LAppDefine.VIEW_LOGICAL_MAX_TOP);
			bg.setUVRect(0.0f,1.0f,0.0f,1.0f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
}