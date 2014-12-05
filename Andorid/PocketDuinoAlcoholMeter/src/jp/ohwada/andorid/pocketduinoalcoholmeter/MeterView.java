/**
 * PocketDuino Alcohol Meter
 * K.OHWADA 2014-12-01
 */

package jp.ohwada.andorid.pocketduinoalcoholmeter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * MeterView
 */
public class MeterView extends View {

	private final static String TAG  = "MeterView";

    private final static int MAX_VALUE = 1024;
 	private final static int MIN_DEGREE = 20;
 	private final static int MAX_DEGREE = 160;
 	    
    private final static int MARGIN_WIDTH = 10;
    private final static int MARGIN_TOP = 50;
    private final static int HAND_WIDTH_HALF = 5;   
 	private final static float CENTER_RATIO = 1.0f / 2.0f;
 	
 	private final static float FRAME_START = 180 + MIN_DEGREE;
 	private final static float FRAME_SWEEP = MAX_DEGREE - MIN_DEGREE;
 	private final static boolean FRAME_CENTER = true; 	    
	private final static int FRAME_STROKE_WIDTH = 5;
 	private final static float DEGREE_RATIO =  FRAME_SWEEP / MAX_VALUE;

 	private final static int TEXT_SIZE = 80;
 	 	    
    private Paint mPaintFrame;
    private Paint mPaintCenter;
    private Paint mPaintMax;
	private Paint mPaintCurrent;
	private Paint mPaintText;

	private RectF mOvalOutside;
	private RectF mOvalInside;
	private RectF mOvalCenter;
	private RectF mRectHand;

 	private int mCenterX = 0;    	
 	private int mCenterY = 0;

 	private float mCurrentDegree = 0;
 	private float mMaxDegree = MIN_DEGREE;

 	private int mTextX = 0;
 	private int mTextY = 0;
	private String mText = "";
	  	
    /** 
	 * === constructor ===
	 */	
	public MeterView( Context context, AttributeSet attrs, int defStyleAttr ) {
     	super( context, attrs, defStyleAttr );
     	initView( context );
	}

    /** 
	 * === constructor ===
	 */	
	public MeterView( Context context, AttributeSet attrs ) {
     	super( context, attrs );
     	initView( context );
    }
     	
    /** 
	 * === constructor ===
	 */	        
	public MeterView( Context context ) {
	    super( context );
     	initView( context );
	}

    /** 
	 * initView
	 */	 
	private void initView( Context context ) {
		mPaintFrame = new Paint();
		mPaintFrame .setColor( Color.BLACK );
 		mPaintFrame.setStyle( Style.STROKE );
 		mPaintFrame.setStrokeWidth( FRAME_STROKE_WIDTH );
		mPaintCenter = new Paint();
		mPaintCenter .setColor( Color.WHITE );
		mPaintMax = new Paint();
		mPaintMax .setColor( Color.RED );
		mPaintMax.setAntiAlias( true );
		mPaintCurrent = new Paint();
		mPaintCurrent .setColor( Color.BLUE );
		mPaintText = new Paint();
		mPaintText .setColor( Color.BLACK );
		mPaintText.setAntiAlias( true );
		mPaintText.setTextSize( TEXT_SIZE );
	}

    /** 
	 * === onWindowFocusChanged ===
	 */	
	@Override  
	public void onWindowFocusChanged( boolean hasFocus ) {  
		super.onWindowFocusChanged( hasFocus );  
		int width = getWidth();  

    	mCenterX = width / 2;    	
    	mCenterY = mCenterX + MARGIN_TOP;  
    	mTextY = mCenterY;  

		float o_width_half =  width / 2 - MARGIN_WIDTH;				    	
		float o_left = MARGIN_WIDTH;
		float o_right = width - MARGIN_WIDTH;
		float o_top = mCenterY - o_width_half; 
		float o_bottom = mCenterY + o_width_half; 
    	mOvalOutside  = new RectF( o_left, o_top, o_right, o_bottom );
 
		float i_width_half = CENTER_RATIO * o_width_half;
		float i_left = mCenterX - i_width_half;
		float i_right = mCenterX + i_width_half;
		float i_top = mCenterY - i_width_half;
		float i_bottom = mCenterY + i_width_half;
    	mOvalInside  = new RectF( i_left, i_top, i_right, i_bottom );

		float c_offset = FRAME_STROKE_WIDTH + 2;
		float c_top = i_top + c_offset;
		float c_bottom = i_bottom + c_offset;
    	mOvalCenter  = new RectF( i_left, c_top, i_right, c_bottom );

		float h_left = o_left;     	
		float h_right = i_width_half + h_left; 
		float h_top = mCenterY - HAND_WIDTH_HALF;
		float h_bottom = mCenterY + HAND_WIDTH_HALF;
    	mRectHand  = new RectF( h_left, h_top, h_right, h_bottom );
	} 
 	
    /** 
     * === onDraw ===
     */	        
	@Override
    protected void onDraw( Canvas canvas ) {
    	// current meter
  	   	canvas.drawArc( mOvalOutside, FRAME_START, mCurrentDegree, FRAME_CENTER, mPaintCurrent ); 
     	canvas.save();
     	// max hand
 		canvas.rotate( mMaxDegree, mCenterX, mCenterY );
		canvas.drawRect( mRectHand, mPaintMax );
  	   	canvas.restore();
  	   	// frame
    	canvas.drawArc( mOvalOutside, FRAME_START, FRAME_SWEEP, FRAME_CENTER, mPaintFrame ); 
    	 canvas.drawArc( mOvalInside, FRAME_START, FRAME_SWEEP, FRAME_CENTER, mPaintFrame );     
  	   	canvas.drawArc( mOvalCenter, FRAME_START, FRAME_SWEEP, FRAME_CENTER, mPaintCenter );   
  	   	// current text  	
  	   	canvas.drawText( mText, mTextX, mTextY, mPaintText );  
	}

    /** 
     * updateMax
     */
	public void updateMax( int max ) {
		mMaxDegree = DEGREE_RATIO * max + MIN_DEGREE;
        invalidate();       
	}

    /** 
     * updateCurrent
     */
	public void updateCurrent( int current ) {
		mCurrentDegree = DEGREE_RATIO * current;
		mText = String.valueOf( current );
		mTextX = (int) ( mCenterX - mPaintText.measureText( mText  ) / 2 );
        invalidate();       
	}
	    
	/**
	 * log_d
	 */	
	@SuppressWarnings("unused")
	private void log_d( String str ) {
		Log.d( TAG, str );
	}
}