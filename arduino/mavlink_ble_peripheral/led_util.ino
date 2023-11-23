

void ledPinSetup() {
  pinMode(LEDR, OUTPUT);
  pinMode(LEDG, OUTPUT);
  pinMode(LEDB, OUTPUT);
  pinMode(LED_BUILTIN, OUTPUT);
  setColor(-1);
}

void setColor(int color) {
   switch (color) {
      case COLOR_RED:
        digitalWrite(LEDR, LOW);
        digitalWrite(LEDG, HIGH);
        digitalWrite(LEDB, HIGH);
        digitalWrite(LED_BUILTIN, LOW);
        break;
      case COLOR_GREEN:
        digitalWrite(LEDR, HIGH);
        digitalWrite(LEDG, HIGH);
        digitalWrite(LEDB, LOW);
        digitalWrite(LED_BUILTIN, LOW);
        break;
      case COLOR_BLUE:
        digitalWrite(LEDR, HIGH);
        digitalWrite(LEDG, LOW);
        digitalWrite(LEDB, HIGH);
        digitalWrite(LED_BUILTIN, LOW);
        break;
      case COLOR_WHITE:
        digitalWrite(LEDR, HIGH);
        digitalWrite(LEDG, HIGH);
        digitalWrite(LEDB, HIGH);
        digitalWrite(LED_BUILTIN, HIGH);
        break;
      default:
        digitalWrite(LEDR, HIGH);
        digitalWrite(LEDG, HIGH);
        digitalWrite(LEDB, HIGH);
        digitalWrite(LED_BUILTIN, LOW);
        break;
    }      
}