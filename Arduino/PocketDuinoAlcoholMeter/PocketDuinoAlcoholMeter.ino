/**
 * PocketDuino AlcoholMeter
 * K.OHWADA 2014-12-01
 */

// message
//   "Sxxx" : Alcohol sensor value

#define PIN_SENSOR  A0
#define SPEED 9600
#define LOOP_DELAY 1000

int sensorValue; 
 
void setup() { 
  Serial.begin( SPEED ); 
} 

void loop() { 
	// read 
	sensorValue = analogRead( PIN_SENSOR );
	// start mark
	Serial.print( "s" ); 
	// sensor value
	Serial.print( sensorValue, DEC );
	// end mark
	Serial.print( "\r" ); 
	// delay
	delay( LOOP_DELAY  ); 
}
