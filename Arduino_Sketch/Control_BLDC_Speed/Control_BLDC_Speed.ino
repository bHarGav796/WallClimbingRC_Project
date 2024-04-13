#include <Servo.h>
#include <SoftwareSerial.h>

SoftwareSerial mySerial(0, 1);
Servo esc; // declare motor as esc

void setup() {
  mySerial.begin(9600);
  esc.attach(10); // motor signal pin attached to pin 10
  esc.write(90);  // initial throttle value of motor, adjust as needed
  delay(3000);    // delay for 3 sec
}

void loop() {
  if (mySerial.available()) {
    char x = mySerial.read();
    
    // Manually map the received value to a range suitable for your servo
    int mappedValue = map(x, '0', '9', 0, 180); // adjust the range as needed
    
    // Use write to set the servo position
    esc.write(mappedValue);
  }
}
