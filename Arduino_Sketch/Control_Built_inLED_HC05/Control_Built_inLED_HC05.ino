
#include<SoftwareSerial.h>
SoftwareSerial mySerial(0,1);

void setup() {
  mySerial.begin(9600);
  Serial.begin(115200);

  pinMode(LED_BUILTIN, OUTPUT);
}

// the loop function runs over and over again forever
void loop() {

    if(mySerial.available()){
      char x = mySerial.read();
      switch (x) {
      case 'H':
      case 'h':
        digitalWrite(LED_BUILTIN, HIGH); // Turn the LED on
        break;
      case 'L':
      case 'l':
        digitalWrite(LED_BUILTIN, LOW); // Turn the LED off
        break;
    }
      Serial.print(x);
    }
} 
