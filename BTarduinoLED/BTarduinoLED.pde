#include <NewSoftSerial.h>   //Software Serial Port Better lib then the other serial libaries

//define the recive and transmit pins for the bluetoothbe
//have the switch set to 3.3v wire vcc to 3.3v on arduino and gruond to ground
#define RxD 6
#define TxD 7

//these are the pins for the led must be PWM pins to control brightness
#define redpin 9
#define greenpin 10
#define bluepin 11

//define variables to hold the integer value of the recived data
int redvalue=0;
int greenvalue=0;
int bluevalue=0;


//Base connection method for a slave device taken from 
//http://garden.seeedstudio.com/index.php?title=Bluetooth_Bee
NewSoftSerial blueToothSerial(RxD,TxD);
 
void setupBlueToothConnection(){
    Serial.println("Setting up Bluetooth Link");
    delay(1000);
    blueToothSerial.begin(38400); //Set BluetoothBee BaudRate to default baud rate 38400
    delay(1000);
    sendBlueToothCommand("\r\n+INQ=0\r\n");
    Serial.println("Sending: +INQ=0");
    delay(2000);
    sendBlueToothCommand("\r\n+STWMOD=0\r\n");
    Serial.println("Sending: +STWMOD=0");
    sendBlueToothCommand("\r\n+STNA=ArduinoBT\r\n");
    Serial.println("Sending: +STNA=ArduinoBT");
    sendBlueToothCommand("\r\n+STAUTO=0\r\n");
    Serial.println("Sending: +STAUTO=0");
    sendBlueToothCommand("\r\n+STOAUT=1\r\n");
    Serial.println("Sending: +STOAUT=1");
    sendBlueToothCommand("\r\n+STPIN=0000\r\n");
    Serial.println("Sending:+STPIN=0000");
    delay(2000); // This delay is required.
    sendBlueToothCommand("\r\n+INQ=1\r\n");
    Serial.println("Sending: +INQ=1");
    delay(2000); // This delay is required.
    blueToothSerial.flush();
}
 
//Checks if the response "OK" is received.
void CheckOK(){
  char a,b;
  while(1){
    if(int len = blueToothSerial.available()){
      a = blueToothSerial.read();
      if('O' == a){
        b = blueToothSerial.read();
        if('K' == b){
          Serial.println("BLUETOOTH OK");
          while( (a = blueToothSerial.read()) != -1){
            Serial.print(a);
          }
          break;
        }
      }
    }
  }
  while( (a = blueToothSerial.read()) != -1){
    Serial.print(a);
  }
}
 
//Send the command to Bluetooth Bee
void sendBlueToothCommand(char command[]){
    blueToothSerial.print(command);
    CheckOK();   
}

void setup(){ 
  //standard setup for pins
  pinMode(RxD, INPUT);
  pinMode(TxD, OUTPUT);
  Serial.begin(9600);
  pinMode(redpin, OUTPUT);
  pinMode(greenpin, OUTPUT);
  pinMode(bluepin, OUTPUT);
  setupBlueToothConnection();
} 


void loop() {
  //check if more then 2 bytes are avialable as we are after 3 bytes as the app inventor code is sending the 3 brightness levels 0 to 255 as single bytes each
  if ( blueToothSerial.available() > 2){
    if ( blueToothSerial.available() == 3){  //make sure its 3 bytes
      //read each of the 3 bytes for brightness into the variables
      redvalue=blueToothSerial.read(); 
      greenvalue=blueToothSerial.read();
      bluevalue=blueToothSerial.read();
      blueToothSerial.flush();

      //debug output the values
      Serial.print("RED: ");
      Serial.println(redvalue,DEC);
      Serial.print("GREEN: ");
      Serial.println(greenvalue,DEC);
      Serial.print("BLUE: ");
      Serial.println(bluevalue,DEC);
      
      blueToothSerial.println("The following data was recived from AI proccessed by the arduino and sent back");
      blueToothSerial.print("RED: ");
      blueToothSerial.println(redvalue,DEC);
      blueToothSerial.print("GREEN: ");
      blueToothSerial.println(greenvalue, DEC);
      blueToothSerial.print("BLUE: ");
      blueToothSerial.println(bluevalue);
    //if the data is not 3 bytes treat it as invalid and warn then flush the buffer.
    } else {
      blueToothSerial.println("Invalid Data was recived");
      Serial.println("Invalid Data was recived");
      char a;
      while( (a = blueToothSerial.read()) != -1){
        Serial.print(a);
      }
    }
      
  } 
  //always write the current values to the pwm pins.
  analogWrite(redpin,redvalue);
  analogWrite(greenpin,greenvalue);
  analogWrite(bluepin,bluevalue);
} 
