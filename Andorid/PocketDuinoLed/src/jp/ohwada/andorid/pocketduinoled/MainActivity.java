/**
 * PocketDuino Led
 * K.OHWADA 2014-12-01
 */

// command
//   "Lx" : trun on/off LED
// message
//   "Sx" : LED status

package jp.ohwada.andorid.pocketduinoled;

import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

/**
 * MainActivity
 */ 
public class MainActivity extends Activity {
	
    private static final String TAG = "pocketduino";
    private static final boolean D = true;

    private static final String LF= "\n"; 

    private static final String ACTION_USB_PERMISSION = "USB_PERMISSION";

	private static final int OPEN_CLOSE = 0;
	private static final int OPEN_SUCCESS = 1;
	private static final int OPEN_FAIL = 2;

    private Physicaloid mPhysicaloid;
    private Handler mHandler; 
    
    private Button mButtonStart;      
    private TextView mTextViewStatus;

	private Button mButtonLed;    
    private TextView mTextViewSwitch;

	private String mRead = "";
	
	// LED Status
	private boolean isLed = false;
	
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
		mButtonLed = (Button) findViewById( R.id.Button_led );
		mTextViewSwitch = (TextView) findViewById( R.id.TextView_switch );
								        
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
		unregisterReceiver( mBroadcastReceiver );
    } 

	/**
	 * === onClickStart ===
	 */ 
    public void onClickStart( View v ) {
    	openDeviceWithStatus();
    }	

	/**
	 * === onClickLed === 
	 */
	public void onClickLed( View v ) {
		String str = "";
		if ( isLed ) {
			mButtonLed.setText( R.string.button_on );
			mButtonLed.setTextColor( Color.RED );
			str = "L1" ;
		} else {
			mButtonLed.setText( R.string.button_off );
			mButtonLed.setTextColor( Color.BLACK );
			str = "L0";
		}
		write( str );
		isLed = !isLed;
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
    }
	
	/**
	 * closeDevice
	 */
    private void closeDevice() {
		if ( mPhysicaloid.isOpened() ) { 
        	mPhysicaloid.close(); 
            mPhysicaloid.clearReadListener();
        }
    }

	/**
	 * write
	 */ 
	private void write( String str ) {
		if ( !mPhysicaloid.isOpened() ) return;
		str += LF;
		byte[] buf = str.getBytes();
		mPhysicaloid.write( buf, buf.length );
	}
	
	/**
	 * execRead
	 */ 
	private void execRead( int size ) { 
	   if ( size < 3 ) return;
    	byte[] buf = new byte[ size ]; 
        mPhysicaloid.read( buf, size ); 	
        String str = byteToString( buf );	
        log_d( "execRead " + str );
        if ( str == null ) return;
        mRead = str;		
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
		if ( mRead == null ) return;		
		if ( mRead.indexOf( "S0" ) != -1 ) {
			mTextViewSwitch.setText( R.string.switch_off );	
			mTextViewSwitch.setTextColor( Color.BLACK );
		} else if ( mRead.indexOf( "S1" ) != -1 ) {
			mTextViewSwitch.setText( R.string.switch_on );	
			mTextViewSwitch.setTextColor( Color.RED );
		}			
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
