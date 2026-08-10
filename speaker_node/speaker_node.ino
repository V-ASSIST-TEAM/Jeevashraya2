#include <ESP8266WiFi.h>

extern "C"
{
#include <espnow.h>
}

#define BUZZER D5

typedef struct
{
  char message[20];
} AlertMessage;

AlertMessage incoming;

void buzzerAlert()
{
  for(int i=0;i<5;i++)
  {
    digitalWrite(BUZZER,HIGH);
    delay(300);

    digitalWrite(BUZZER,LOW);
    delay(300);
  }
}

bool alertReceived = false;

void onDataRecv(uint8_t *mac, uint8_t *data, uint8_t len)
{
    memcpy(&incoming, data, min((int)len, (int)sizeof(incoming)));

    if(strcmp(incoming.message, "FALL ALERT") == 0)
    {
        alertReceived = true;
    }
}

void loop()
{
    if(alertReceived)
    {
        alertReceived = false;

        Serial.println("ALERT RECEIVED");

        buzzerAlert();
    }
}

void setup()
{
  Serial.begin(115200);

  pinMode(BUZZER,OUTPUT);
  digitalWrite(BUZZER,LOW);

  WiFi.mode(WIFI_STA);
  Serial.print("Channel: ");
  Serial.println(wifi_get_channel());

  Serial.print("MAC: ");
  Serial.println(WiFi.macAddress());

  if(esp_now_init()!=0)
  {
    Serial.println("ESP-NOW Init Failed");
    return;
  }

  esp_now_set_self_role(ESP_NOW_ROLE_SLAVE);

  esp_now_register_recv_cb(onDataRecv);

  Serial.println("Waiting for Alert...");
}

