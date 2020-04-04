//
// Created by Bekzhan Kaspakov on 4/12/18.
//

#include <ntsid.h>

#ifndef TESTSELECT_MAIN_H
#define TESTSELECT_MAIN_H

#endif //TESTSELECT_MAIN_H
typedef struct{
    int fd;
    char username[200];
    char groupname[200];
    int score;
}player;

typedef struct{
    char topic[200];
    char groupname[200];
    int Lnum;
    int j;
    player member[100];
    int valid;
    int admin;
    fd_set gfds;
    int			nfds;
    fd_set *MainMenufds;

}group;
pthread_mutex_t lock;
group groups[32];
