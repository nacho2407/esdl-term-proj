#ifndef __GAME_H
#define __GAME_H

#include <stdint.h>
#include "protocol.h"

void Game_Init(void);
void Game_Loop(void);              // main.c���� ���
uint32_t Game_GetRecentPeak(void); // trigger.c���� ���

#endif
