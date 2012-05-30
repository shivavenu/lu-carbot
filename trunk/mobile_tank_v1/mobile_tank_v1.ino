#include <AFMotor.h>
#include <Servo.h>

AF_DCMotor track1(4);  // left trank
AF_DCMotor track2(1);  // right trank
Servo myservo; // create servo object to control a servo
int maxSpeed = 200; // maximum speed of motors
boolean isMovingForward = false;

const int switch1 = A1; // left switch pin
const int switch2 = A2; // right switch pin
const int pingPin = A0; // PING distance sensor data pin
const int msgLen = 50;  // message buffer length

int msgIndex = 0; // next message index
char message[msgLen]; // message buffer
float x = 0, y = 0, z = 0; // orientation data
float mx = 0, my = 0;      // touch screen data

void setup()
{
  // init pins and motors
  pinMode(switch1, OUTPUT);
  pinMode(switch2, OUTPUT);
  track1.run(RELEASE);
  track2.run(RELEASE);
  myservo.attach(10, 800, 2400);

  // start serial channel (for BT)
  Serial.begin(57600);

  // init message buffer
  for (int i=0; i<msgLen; i++) message[i] = 0;
}
s
void loop()
{
  // receive message from BT dongle
  while (Serial.available() > 0)
  {
    int r = Serial.read();
    if (r != 0x13) message[msgIndex++] = r; // normal case: add new char into message array
    else if (message[0] == 'A') decodeMessageA(); // decode orientation data
    else decodeMessageB(); // decode touch screen data
  }

  // main logic
  if (digitalRead(switch1) || digitalRead(switch2)) { 
    moveBackward(maxSpeed); 
    delay(500); 
  }
  else if (y < -4) turnLeft(map(-4 - y, 0, 5, 100, maxSpeed));
  else if (y > 4) turnRight(map(y - 4, 0, 5, 100, maxSpeed));
  else if (z > 5) moveForward(map(z - 5, 0, 5, 100, maxSpeed));
  else if (z < -2) moveBackward(map(-2 - z, 0, 5, 100, maxSpeed));
  else stopMoving();

  if (getDistance() < 10 && isMovingForward) stopMoving();
  delay(10);
}

void moveForward(int sp)
{
  isMovingForward = true;
  track1.setSpeed(sp);
  track2.setSpeed(sp);
  track1.run(BACKWARD);
  track2.run(BACKWARD);
}

void moveBackward(int sp)
{
  isMovingForward = false;
  track1.setSpeed(sp);
  track2.setSpeed(sp);
  track1.run(FORWARD);
  track2.run(FORWARD);
}

void stopMoving()
{
  isMovingForward = false;
  track1.run(RELEASE);
  track2.run(RELEASE);
}

void turnLeft(int sp)
{
  isMovingForward = false;
  track1.setSpeed(sp);
  track2.setSpeed(sp);
  track1.run(FORWARD);
  track2.run(BACKWARD);
}

void turnRight(int sp)
{
  isMovingForward = false;
  track1.setSpeed(sp);
  track2.setSpeed(sp);
  track1.run(BACKWARD);
  track2.run(FORWARD);
}

void decodeMessageA()
{
  int index1;
  int index2;
  boolean first = true;

  for (int i=1; i<msgIndex; i++)
    if (message[i] == ';')
    {
      message[i] = 0;
      if (first) { 
        index1 = i+1; 
        first = false; 
      }
      else { 
        index2 = i+1; 
      }
    }
  x = atof(message + 1);
  y = atof(message + index1);
  z = atof(message + index2);
  /*
  Serial.print(x);
   Serial.print(" ");
   Serial.print(y);
   Serial.print(" ");
   Serial.println(z);
   */

  for (int i=0; i<msgIndex; i++) 
    message[i] = 0;
  msgIndex = 0;
}

void decodeMessageB()
{
  int index;

  for (int i=1; i<msgIndex; i++)
    if (message[i] == ';')
    {
      message[i] = 0;
      index = i+1;
    }
  mx = atof(message + 1);
  my = atof(message + index);
  float pos = map(mx, 0, 500, 30, 100);
  myservo.write(int(pos));

  for (int i=0; i<msgIndex; i++) 
    message[i] = 0;
  msgIndex = 0;
}

// return distance from ping sensor
long getDistance()
{
  long duration;

  // generate a HIGH pulse 
  pinMode(pingPin, OUTPUT);
  digitalWrite(pingPin, LOW);
  delayMicroseconds(2);
  digitalWrite(pingPin, HIGH);
  delayMicroseconds(5);
  digitalWrite(pingPin, LOW);

  // wait a HIGH pulse
  pinMode(pingPin, INPUT);
  duration = pulseIn(pingPin, HIGH);

  // convert the time into a distance
  return duration / 29 / 2;
}




