/**
 * PocketDuino Led
 * K.OHWADA 2014-12-01
 */

// command
//   "Lx" : trun on/off LED
// message
//   "Sx" : LED status

#define PIN_LED 13
#define SPEED 9600
#define LOOP_DELAY 100

#define MAX_SERIAL_WAIT  100
#define SERIAL_DELAY 1
#define ERR  -1

void setup() {
	Serial.begin( SPEED );
	pinMode( PIN_LED, OUTPUT );  
}

void loop() {
	// if recieve serial 
	if ( Serial.available() ) {
		int c = Serial.read();
		// echo back
//		Serial.write( c );
		if ( c == 'L' ) {
			execLed();
		}
	}
	delay( LOOP_DELAY  ); 
}

void execLed() {
	int value = recvOneDigit();
	if ( value == 0 ) {
		// LED on if "0"
		digitalWrite( PIN_LED, LOW ); 
		// LED status
		Serial.println( "S0" );
	} else if ( value == 1 ) {
		// LED on if "1"
		digitalWrite( PIN_LED, HIGH ); 
		// LED status
		Serial.println( "S1" );
	} 
}

int recvOneDigit() {
	int cnt = 0;
	// wait next char
	while( true ) {
		// if recieve serial 
  		if ( Serial.available() ) {
			int c = Serial.read();
			// echo back
//			Serial.write( c );
			int value = c - '0';
			if ( value < 0 || 9 < value ){
				return ERR;
			}
 			return value;
  		}
		// when wait 100 msec
  		if ( cnt > MAX_SERIAL_WAIT ) {
    		return ERR;
  		}
		cnt ++;
  		delay( SERIAL_DELAY );
	}   
}
