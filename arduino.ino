/*
  PRANASETU AI - Arduino UNO event sender
  Replace the example sensor logic with your actual sensors.

  Serial format:
  INTRUDER:0
  INTRUDER:1
  FIRE:0
  FIRE:1
  BLUE_CARBON:OK
  ALGAE:OK
*/

const int PIR_PIN = 2;
const int FLAME_PIN = 3;
const int SMOKE_PIN = A0;

void setup() {
  Serial.begin(9600);
  pinMode(PIR_PIN, INPUT);
  pinMode(FLAME_PIN, INPUT);
}

void loop() {
  bool intruder = digitalRead(PIR_PIN) == HIGH;
  bool fire = digitalRead(FLAME_PIN) == LOW; // change logic if your sensor is different

  Serial.print("INTRUDER:");
  Serial.println(intruder ? 1 : 0);

  Serial.print("FIRE:");
  Serial.println(fire ? 1 : 0);

  // Example environmental module states.
  Serial.println("BLUE_CARBON:OK");
  Serial.println("ALGAE:OK");

  delay(1000);
}
