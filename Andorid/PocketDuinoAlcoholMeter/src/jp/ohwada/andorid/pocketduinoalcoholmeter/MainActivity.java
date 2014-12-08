/**
 * PocketDuino Alcohol Meter
 * K.OHWADA 2014-12-01
 */

// message
// "Sxxx" : Alcohol sensor value

package jp.ohwada.andorid.pocketduinoalcoholmeter;

import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

/**
 * MainActivity
 */ 
public class MainActivity extends Activity {
	
    private static final String TAG = "alcoholmeter";
    private static final boolean D = true;
 
    private static final String ACTION_USB_PERMISSION = "USB_PERMISSION";
 	private static final int NUM_RANK = 5;    

	private static final int OPEN_CLOSE = 0;
	private static final int OPEN_SUCCESS = 1;
	private static final int OPEN_FAIL = 2;
	private static final int READ_ERROR = -1;

    private Physicaloid mPhysicaloid;
    private Handler mHandler; 
    
    private Button mButtonStart;      
    private TextView mTextViewStatus; 
    private TextView[] mTextViewRanks = new TextView[ NUM_RANK ];
    private MeterView  mMeterView;

    private int	mMax = 0;
	private int	mCurrent = 0;
    private int[] mRanks = new int[ NUM_RANK ];
    private boolean isMaxChange = false;

	/**
	 * === onCreate ===
	 */     
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        log_d( "onCreate" );
        setContentView( R.layout.activity_main );

		mButtonStart  = (Button) findViewById( R.id.Button_start );
		mTextViewStatus  = (TextView) findViewById( R.id.TextView_status );
        mMeterView  = (MeterView) findViewById( R.id.MeterView );

		Resources res = getResources();
		String name = getPackageName();
        for ( int i=0; i<NUM_RANK; i++ ) {
        	String str = "TextView_rank_" + i ;
			int id = res.getIdentifier( str, "id", name );
			mTextViewRanks[ i ] = (TextView) findViewById( id );
        	mRanks[ i ] = 0;
		}
								        
        mPhysicaloid = new Physicaloid( this );
        mHandler = new Handler(); 

        IntentFilter filter = new IntentFilter();
        filter.addAction( UsbManager.ACTION_USB_DEVICE_ATTACHED );
        filter.addAction( UsbManager.ACTION_USB_DEVICE_DETACHED );
        filter.addAction( ACTION_USB_PERMISSION );
        registerReceiver( mBroadcastReceiver, filter );

        int ret = openDevice();
        switch ( ret ) {
        	case OPEN_SUCCESS:
				mButtonStart.setText( R.string.button_stop ); 
        		mTextViewStatus.setText( R.string.status_connected ); 
				break;
			default: 
				mButtonStart.setText( R.string.button_start ); 
        		mTextViewStatus.setText( R.string.status_not ); 
				break;
		}
    }

	/**
	 * === onDestroy ===
	 */ 
    @Override
    protected void onDestroy() { 
        super.onDestroy();
        closeDevice();
		mPhysicaloid.clearReadListener();
		unregisterReceiver( mBroadcastReceiver );
    } 

	/**
	 * === onClickStart ===
	 */ 
    public void onClickStart( View v ) {
    	openDeviceWithStatus();
    }	

	/**
	 * === onClickRank ===
	 */  
    public void onClickRank( View v ) {
    	int[] ranks = new int[ NUM_RANK ];
		boolean is_set = false;
     	int rank;
    	for ( int i=0; i<NUM_RANK; i++ ) {
    		 if ( is_set ) {	
        		rank = mRanks[ i - 1 ];
        	} else if ( mMax > mRanks[ i ] ) {
    			rank =  mMax;
    			is_set = true;
    		} else {
    			rank = mRanks[ i ];
    		}
    		ranks[ i ] = rank;
    		mTextViewRanks[ i ].setText( String.valueOf( rank ) );	
        }
        mRanks = ranks;
    	mMax = 0;
    }
    
	/**
	 * === onClickClear ===
	 */  
    public void onClickClear( View v ) {
    	mMax = 0;
    }	

	/**
	 * openDevice WithStatus
	 */
    private void openDeviceWithStatus() {
        int ret = openDevice();
        switch ( ret ) {
        	case OPEN_SUCCESS:
				mButtonStart.setText( R.string.button_stop ); 
        		mTextViewStatus.setText( R.string.status_connected ); 
				break;
        	case OPEN_FAIL:
				mButtonStart.setText( R.string.button_start ); 
        		mTextViewStatus.setText( R.string.status_cannot ); 
				break;
			default: 
				mButtonStart.setText( R.string.button_start ); 
        		mTextViewStatus.setText( R.string.status_not ); 
				break;
		}
    }
    	
	/**
	 * openDevice
	 */
    private int openDevice() {
    	int ret = OPEN_CLOSE;
		if ( !mPhysicaloid.isOpened() ) { 
			if ( mPhysicaloid.open() ) { 
                mPhysicaloid.addReadListener( new ReadLisener() { 
                    @Override
                    public void onRead( int size ) { 
                    	execRead( size ); 
                    }
                });
            	ret = OPEN_SUCCESS;                  
            } else { 
            	ret = OPEN_FAIL;
            } 
        } else { 
            mPhysicaloid.close(); 
        } 
        return ret;
    }

	/**
	 * closeDevice WithStatus
	 */
    private void closeDeviceWithStatus() {
        closeDevice();
        mTextViewStatus.setText( R.string.status_not ); 
        mMeterView.updateCurrent( 0 );  
        mMeterView.updateMax( 0 );
    }
	
	/**
	 * closeDevice
	 */
    private void closeDevice() {
		if ( mPhysicaloid.isOpened() ) { 
        	mPhysicaloid.close(); 
        }
    }

	/**
	 * execRead
	 */ 
	private void execRead( int size ) { 
    	byte[] buf = new byte[ size ]; 
        mPhysicaloid.read( buf, size ); 				
		Integer num = decodePacket( buf ); 
		if ( num == READ_ERROR ) return;
		mCurrent = num;
		if ( num > mMax ) {
			mMax = num;
			isMaxChange = true;
		}	
		mHandler.post( new Runnable() { 
			@Override
			public void run() { 
				execRun();
			} 
		}); 
	}

	/**
	 * execRun
	 */
	private void execRun() { 
		mMeterView.updateCurrent( mCurrent ); 
		if ( !isMaxChange ) return; 

		mMeterView.updateMax( mMax ); 
		boolean is_set = false;
		int num = NUM_RANK - 1;
		for ( int i=0; i<num; i++ ) {
			if ( is_set ) {
				setTextRank( i, mRanks[ i - 1 ], Color.BLACK );		
			} else if ( mMax > mRanks[ i ] ) {
				setTextRank( i, mMax, Color.RED );
				is_set = true;
			} else {
				setTextRank( i, mRanks[ i ], Color.BLACK );
			}	
		}
		if ( is_set ) {
			setTextRank( num, mRanks[ num - 1 ], Color.BLACK );	
		} else {
			setTextRank( num , mMax, Color.RED );	
		}		
		isMaxChange = false;
	}

	/**
	 * setTextRank
	 */
	private void setTextRank( int n, int value, int color ) {
		mTextViewRanks[ n ].setText(  String.valueOf( value ) );
		mTextViewRanks[ n ].setTextColor( color );
	}
						
	/**
	 * decodePacket
	 * pull out a number between 's' and '\r'
	 */    
    private int decodePacket( byte[] buf ) { 
    	if ( !isUtf8( buf ) ) return READ_ERROR;
        boolean is_start = false; 
        int result = 0;  
        byte c;
        for( int i=0; i<buf.length; i++ ) { 
        	c = buf[i];
            if( !is_start ) { 
            	// s : start
                if( c == 's' ) {
                    is_start = true; 
                } 
            } else { 
            	// \r : end
                if( c == '\r')  { 
                    return result; 
                } else { 
                    if( '0' <= c && c <= '9' ) {
                    	// save to result while shifting the number
                    	// convert to a number from ASCII code by subtracting the character ' 0'
                        result = ( 10 * result ) + ( c - '0' ); 
                	} else { 
                        return READ_ERROR; 
                    } 
                } 
            } 
        } 
        return READ_ERROR; 
    } 

	/**
	 * isUtf8
	 */
    private boolean isUtf8( byte[] buf ) { 
    	String str = byteToString( buf );
    	if ( str == null ) {
    		return false;
    	}
		return true;
	} 

	/**
	 * byteToString
	 */
    private String byteToString( byte[] buf ) { 
    	String str = null;
		try { 
			str = new String( buf, "UTF-8" ); 
		} catch ( UnsupportedEncodingException e ) { 
			if (D) e.printStackTrace(); 
		}
		return str;
	} 

	/**
	 * BroadcastReceive
	 */
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive( Context context, Intent intent ) {
            String action = intent.getAction();
            log_d( "onReceive " + action );
            if ( UsbManager.ACTION_USB_DEVICE_ATTACHED.equals( action ) ) {
            	toast_show( R.string.toast_attach );
                openDeviceWithStatus();
            } else if ( ACTION_USB_PERMISSION.equals( action ) ) {
            	// TODO : not occur this callback
            	toast_show( R.string.toast_permit );
				openDeviceWithStatus();
            } else if ( UsbManager.ACTION_USB_DEVICE_DETACHED.equals( action ) ) {
            	toast_show( R.string.toast_detach );
                closeDeviceWithStatus();
            }
        }
    };

	/**
	 * toast_show
	 * @param int res_id
	 */ 
	private  void toast_show( int res_id ) {
		ToastMaster.showText( this, res_id, Toast.LENGTH_SHORT );
	}
                	
	/**
	 * log_d
	 */
	private void log_d( String msg ) {	
		if (D) Log.d( TAG, msg ); 
	}	
}
