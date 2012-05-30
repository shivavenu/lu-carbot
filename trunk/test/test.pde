#include <NewSoftSerial.h> 
#include <MeetAndroid.h>
#include <Servo.h>
#define RxD 6
#define TxD 7

//MeetAndroid meetAndroid;
Servo servoR;

//#define DEBUG_ENABLED  1
byte msg = 0;
 
NewSoftSerial blueToothSerial(RxD,TxD);
void setup()  
{
  pinMode(RxD, INPUT);
  pinMode(TxD, OUTPUT); 
  setupBlueToothConnection();
  servoR.attach(22);
}


void loop()
{
  //meetAndroid.receive(); // you need to keep this in your loop() to receive events
  
  if ( blueToothSerial.available() > 2){
    if ( blueToothSerial.available() == 3){  //make sure its 3 bytes
      //read each of the 3 bytes for brightness into the variables
      msg=blueToothSerial.read(); 
      blueToothSerial.flush();
      
      switch (msg){
      case 0x0:
        servoR.write(90);
        break;
      }

      //debug output the values
      Serial.print("msg: ");
      Serial.println(msg,DEC);
      
    } else {
      blueToothSerial.println("Invalid Data was recived");
      Serial.println("Invalid Data was recived");
      char a;
      while( (a = blueToothSerial.read()) != -1){
        Serial.print(a);
      }
    }
  }
}
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
void sendBlueToothCommand(char command[]){
    blueToothSerial.print(command);
    CheckOK();   
}
