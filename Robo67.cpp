#include "BluetoothSerial.h"
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <ESP32Servo.h>

BluetoothSerial Serial1BT;

// defino o valor bit referente a cada variavel, o sinal << mostra o local do 1, ou seja 1 << 0 = 00000001 ou 1 << 1 = 00000010

#define up (1 << 0)
#define down (1 << 1)
#define right (1 << 2)
#define left (1 << 3)
#define action (1 << 4)

//config do display

#define LARGURA 128
#define ALTURA 64

//definição dos botões dos servos

#define PINO_SERVO 18
#define PINO_SERVO2 19

//posição inicial dos servos

int posicao67 = 5;
int velocidade67 = 1;

unsigned long tempo67 = 0;

//movimento dos servos

int anguloServo = 90;
int direcaoServo = 1;

unsigned long tempoServo = 0;

int anguloServo2 = 90;
int direcaoServo2 = 1;

unsigned long tempoServo2 = 0;

//display

Adafruit_SSD1306 display(LARGURA, ALTURA, &Wire, -1);

bool actionAtivo = false;
unsigned long tempoInicioAction = 0;

//servos

Servo servo;
Servo servo2;

// Confing Motores
//A motor direito
//B motor esquerdo
const int direcao_A = 13;
const int direcao_B = 14;
const int PWM_A = 16;
const int PWM_B = 17;
int velocidade = 100; // max 255

uint8_t infoA = 0;
void setup() {
//Bluetooth
  Serial.begin(115200);
  //muda o nome do bluetooth 
  Serial1BT.begin("Bluetooth_Robozao");
  //mostra quando o bluetooth esta disponivel
  Serial.println("Ta ligado boboca");
  //serial que liga o arduino
  // Servo 1
  servo.attach(PINO_SERVO);
  servo.write(90);
  // Servo 2
  servo2.attach(PINO_SERVO2);
  servo2.write(90);

  display.begin(SSD1306_SWITCHCAPVCC, 0x3C);

  olhosAbertos();

 // Motores

 pinMode(direcao_A, OUTPUT);
 pinMode(direcao_B, OUTPUT);
 pinMode(PWM_A, OUTPUT);
 pinMode(PWM_B, OUTPUT);

}

void olhosAbertos() {

  display.clearDisplay();

  display.fillRoundRect(20, 18, 40, 25, 8, WHITE);
  display.fillRoundRect(68, 18, 40, 25, 8, WHITE);

  display.display();
}

void mostrar67() {

  display.clearDisplay();

  display.setTextSize(4);
  display.setTextColor(WHITE);

  display.setCursor(35, posicao67);

  display.print("67");

  display.display();
}

void Frente(){
  // Os dois motores sentido horario
  digitalWrite(direcao_A, HIGH); //Motor A. HIGH = HORARIO
  digitalWrite(direcao_B, LOW); //Motor B. LOW = ANTI.HORARIO
  analogWrite(PWM_A, velocidade); //PWM do motor Direito 
  analogWrite(PWM_B, velocidade); //PWM do motor Esquerdo 
}

void Re(){
  // Os dois motores sentido horario
  digitalWrite(direcao_A, LOW); //Motor A. LOW = ANTI.HORARIO
  digitalWrite(direcao_B, HIGH); //Motor B. HIGH = HORARIO
  analogWrite(PWM_A, velocidade); //PWM do motor Direito 
  analogWrite(PWM_B, velocidade); //PWM do motor Esquerdo 
}

void Direita(){
  // Os dois motores sentido horario
  digitalWrite(direcao_A, LOW); //Motor A. LOW = ANTI.HORARIO
  digitalWrite(direcao_B, LOW); //Motor B. LOW = ANTI.HORARIO
  analogWrite(PWM_A, 0); //PWM do motor Direito 
  analogWrite(PWM_B, velocidade); //PWM do motor Esquerdo 
}

void Esquerda(){
  // Os dois motores sentido horario
  digitalWrite(direcao_A, HIGH); //Motor A. HIGH = HORARIO
  digitalWrite(direcao_B, LOW); //Motor B. HIGH = HORARIO
  analogWrite(PWM_A, velocidade); //PWM do motor Direito 
  analogWrite(PWM_B, 0); //PWM do motor Esquerdo 
}

void loop() {
  //todas as coisas feitas com o bluetooth
  if(Serial1BT.available() > 0){
    // define que o valor trabalhado será bit, ou seja oito bits, poderia ser 16 para projetos maiores, ou 32 e assim por diante 
    uint8_t info = Serial1BT.read();
    infoA = info;
    Serial.println("info: ");
    Serial.println(info);
// condicionais de movimento e ação, o tal do six seven ou qualquer que seja(parte do luiz, vou deixar vazio para facilitar para ele implementar)
// o & NÂO É UM ERRO DE LÓGICA, NÃO MUDA, SE NÃO VAI QUEBRAR A COMUNICAÇÃO COM O APP
    if(info & up){
      Frente();   
    }
    if(info & down){
     Re();
    }
    if(info & right){
     Direita();
    }
    if(info & left){
     Esquerda();
    }
    if(infoA & action){
        actionAtivo = true;
        tempoInicioAction = millis();
    }


  // ==============================
  // ACTION ATIVO
  // ==============================

   if(actionAtivo) {

  // ==============================
  // MOVIMENTO DO 67
  // ==============================

  if (millis() - tempo67 >= 30) {

    tempo67 = millis();

    posicao67 += velocidade67;

    if (posicao67 >= 35) {
      velocidade67 = -10;
    }

    if (posicao67 <= 5) {
      velocidade67 = 10;
    }
  }
  // ==============================
  // SERVO 1
  // ==============================

  if (millis() - tempoServo >= 30) {

    tempoServo = millis();

    anguloServo += direcaoServo;

    servo.write(anguloServo);

    if (anguloServo >= 120) {
      direcaoServo = -1;
    }

    if (anguloServo <= 90) {
      direcaoServo = 1;
    }
  }


  // ==============================
  // SERVO 2
  // ==============================

  if (millis() - tempoServo2 >= 30) {

    tempoServo2 = millis();

    anguloServo2 += direcaoServo2;

    servo2.write(anguloServo2);

    if (anguloServo2 >= 120) {
      direcaoServo2 = -1;
    }

    if (anguloServo2 <= 90) {
      direcaoServo2 = 1;
    }
  }


  // ==============================
  // OLED
  // ==============================

  mostrar67();


  // ==============================
  // FINAL DA ACTION
  // ==============================

  if (millis() - tempoInicioAction >= 5000) {

    actionAtivo = false;

    servo.write(90);
    servo2.write(90);

    olhosAbertos();
  }
}
  }
}
 
