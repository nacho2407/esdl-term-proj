#include "game.h"
#include "sensor.h"
#include "time.h"
#include "uart.h"
#include <stdio.h>

static int is_capturing = 0;            // ���� ���� ������ Ȯ���ϴ� �÷���
static uint32_t capture_start_time = 0; // ���� ���� �ð�
static uint32_t current_peak = 0;       // ���� ���� �� �ִ밪 ����
extern volatile uint32_t seq;

void Game_Init(void)
{
    is_capturing = 0;
    current_peak = 0;
}

// Trigger.c ���� ȣ��
void Game_StartCapture(void)
{
    is_capturing = 1;
    capture_start_time = millis();
    current_peak = 0; // ��ũ�� �ʱ�ȭ
}

void Game_Loop(void)
{
    // ���� ��尡 �ƴϸ� �ƹ��͵� �� ��
    if (is_capturing == 0)
        return;

    // 1. ���� ������ �б� (sensor.c�� �ֽŰ�)
    uint16_t currentIdx = ADC_GetWriteIndex();
    uint16_t dataIdx = (currentIdx == 0) ? (ADC_BUF_LEN - 1) : (currentIdx - 1);
    uint16_t sensorVal = adc_buf[dataIdx];

    // 2. �ִ밪 ���� (Peak Hold)
    if (sensorVal > current_peak)
    {
        current_peak = sensorVal;
    }

    // 3. �ð� üũ (120ms ��������?)
    if (millis() - capture_start_time >= 120)
    {
        // 120ms ���� -> ��� ����
        // char buf[32];
        // snprintf(buf, sizeof(buf), "PEAK=%lu\r\n", current_peak);
        char *msg = Protocol_BuildAmbientMessage(seq, current_peak);
        USART1_SendString(msg);
        // ���� ����
        is_capturing = 0;
    }
}
