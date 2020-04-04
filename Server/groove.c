
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <netinet/in.h>
#include <sys/select.h>
#include <pthread.h>
#include <fcntl.h>
#include "main.h"
#define	QLEN			5
#define	BUFSIZE			4096

void *groove(void *arg) {
    group *groups = (group *)arg;
    write(groups->admin, "SENDQUIZ\r\n", strlen("SENDQUIZ\r\n"));
    char			buf[BUFSIZE];
    char buff[2048];
    char que[2048];
    char mess[2200];
    int counter =0;
    int lastMember = groups->Lnum;
    fd_set			rfds;
    fd_set			afds;
    int			fd;
    FILE *file;
    int			cc;
    struct timeval timeout;
    timeout.tv_sec = 1;
    timeout.tv_usec = 0;

    /*FD_ZERO(&afds);
    FD_SET(groups->admin, &afds);
    *//************************************************//*
    *//*SELECT FOR Waiting for an admin to send QUIZ  *//*
    *//************************************************//*
    if (select(groups->admin+1, &afds, (fd_set *)0, (fd_set *)0,
               (struct timeval *)0) < 0)
    {
        fprintf( stderr, "server select: %s\n", strerror(errno) );
        exit(-1);
    }

    if (FD_ISSET(groups->admin,&afds))
    {*/
        int quizSize=0;
        if ((cc = (int) read(groups->admin, buf, BUFSIZE )) <= 0 ) {
            printf("The ADMIN is gone.\n");
            (void) close(groups->admin);
            FD_SET(groups->admin, groups->MainMenufds);
            // lower the max socket number if needed
        } else {
            buf[cc]='\0';
            char *token;

            token = strtok(buf, "|");
            if (strcmp(token, "QUIZ") == 0) {
                char filename[255];
                char buffer[30];

                int leftToRead;


                /************************************************************************/
                /*Create temporary file with time stamp so it is unique for every quiz  */
                /************************************************************************/
                time_t tim;
                time(&tim);
                struct tm* tm_info;
                tm_info = localtime(&tim);
                strftime(buffer, 30, "%Y_%m_%d_%H-%M-%S", tm_info);
                printf("name = %s\n", buffer);
                sprintf(filename,"tempfile_%s.txt", buffer);
                if ((file = fopen(filename, "a+"))==NULL){
                    printf("Failed to Create temp file\n");
                    fflush(stdout);
                }

                /******************************************/
                /*Getting size of the quiz from a creator */
                /******************************************/
                token = strtok(NULL, "|");
                quizSize = (int) strtol(token, (char **)NULL, 10);
                leftToRead = quizSize;
                printf("quiz size =  %d\n",leftToRead);


                /**********************/
                /*Read and store quiz */
                /**********************/


                printf("Proceeded here so far\n");
                fflush(stdout);
                fseek(file,0,SEEK_SET);
                while(leftToRead > 0){


                    if(leftToRead<BUFSIZE){
                        if ((cc = (int) read(groups->admin, token, leftToRead )) <= 0 ) {
                            printf("The ADMIN is gone while giving quiz.\n");
                            (void) fclose(file);
                            FD_SET(groups->admin, groups->MainMenufds);
                            pthread_exit(-1);
                            // lower the max socket number if needed
                        } else {
                            token[strlen(token)]='\0';
                            //printf("Did this shit = %s\n", token);
                            leftToRead -= cc;
                            fprintf(file, "%s", token);
                            fflush(file);
                            //write(file, buf, strlen(buf));
                        }
                    } else{
                        if ((cc = (int) read(groups->admin, token, BUFSIZE )) <= 0 ) {
                            printf("The ADMIN is gone while giving quiz.\n");
                            (void) fclose(file);
                            FD_SET(groups->admin, groups->MainMenufds);
                            pthread_exit(-1);
                            // lower the max socket number if needed
                        } else {
                            token[strlen(token)]='\0';
                            printf("Did other shit\n");
                            leftToRead -= cc;
                            fwrite(token,1,strlen(token),file);
                            //write(file, buf, strlen(buf));
                        }

                    }
                }
                write(groups->admin, "OK\r\n", strlen("OK\r\n"));

            }
        }
//    }
    printf("NEW CHECK POINT\n");
    fflush(stdout);
    /******************************/
    /*WAITING FOR PLAYERS TO JOIN */
    /******************************/
    FD_SET(groups->admin,&groups->gfds);
    if (groups->admin>=groups->nfds){
        groups->nfds = groups->admin+1;
    }
    for (int p= 0;;p++)
    {
        memcpy((char *) &rfds, (char *) &groups->gfds, sizeof(rfds));

        printf("The nfds: %d\n", groups->nfds);
        fflush(stdout);

        if (select(groups->nfds, &rfds, (fd_set *) 0, (fd_set *) 0,
                   &timeout) < 0)
        {
            fprintf(stderr, "server select: %s\n", strerror(errno));
            exit(-1);
        }


        if (groups->j < groups->Lnum)
        {
            if (FD_ISSET(groups->admin, &rfds))
            {
                printf("THE ADMIN HAS SPOKEN\n");
                fflush(stdout);

                char *token;
                // read without blocking because data is there
                if ((cc = read(groups->admin, buf, BUFSIZE)) <= 0)
                {
                    printf("The GROUP CREATER IS GONE.\n");
                    fflush(stdout);
                    FD_CLR(groups->admin, &groups->gfds);
                    // lower the max socket number if needed
                    if (groups->nfds == fd + 1)
                        groups->nfds--;
                }
                buf[cc-2] = '\0';
                if (strcmp(buf, "LEAVE")==0){
                    FD_CLR(groups->admin, &groups->gfds);
                    write(groups->admin, "OK\r\n", strlen("OK\r\n"));
                }
                if (strcmp(buf, "GETOPENGROUPS")==0){
                    char message[1000];
                    strcpy(message, "OPENGROUPS");

                    for (int i = 0; i < 32; ++i) {
                        if (groups[i].valid == 1) {
                            strcat(message, "|");
                            strcat(message, groups[i].topic);
                            strcat(message, "|");
                            strcat(message, groups[i].groupname);
                            char str[12];
                            sprintf(str, "%d", groups[i].Lnum);
                            strcat(message, "|");
                            strcat(message, str);
                            sprintf(str, "%d", groups[i].j);
                            strcat(message, "|");
                            strcat(message, str);
                        }
                    }
                    strcat(message, "\r\n");
                    write(groups->admin, message, strlen(message));
                }
                printf("this what client says%s\n", buf);
                fflush(stdout);

                token = strtok(buf, "|");
                if (strcmp(token, "CANCEL") == 0)
                {
                        write(groups->admin, "OK\r\n", strlen("OK\r\n"));
                        printf("The client has gone.\n");
                        fflush(stdout);
                        FD_CLR(groups->admin, &groups->gfds);
                        FD_SET(groups->admin, groups->MainMenufds);
                        groups->valid=0;
                        for (int i = 0; i < groups->nfds; i++) {
                            if (FD_ISSET(i, &groups->gfds)) {
                                printf("FD_ISSET FOR i = %d\n", i);
                                FD_CLR(i, &groups->gfds);
                                FD_SET(i, groups->MainMenufds);
                                write(i, "ENDGROUP\r\n", strlen("ENDGROUP\r\n"));
                            }
                        }
                    pthread_exit(1);


                }

                printf("The client says: %s\n", buf);
                fflush(stdout);
            }

            for (fd = 3; fd < groups->nfds; fd++)
            {

                // check every socket to see if it's in the ready set
                if (fd != groups->admin && FD_ISSET(fd, &rfds))
                {

                    // read without blocking because data is there
                    if ((cc = read(fd, buf, BUFSIZE)) <= 0)
                    {
                        printf("The client has gone.\n");
                        fflush(stdout);
                        pthread_mutex_lock(&lock);

                        groups->j--;
                        pthread_mutex_unlock(&lock);
                        FD_CLR(fd, &groups->gfds);
                        // lower the max socket number if needed
                        if (groups->nfds == fd + 1)
                            groups->nfds--;
                    }
                    buf[cc] = '\0';

                    printf("The client says: %s\n", buf);
                    fflush(stdout);
                    if (strcmp(buf, "LEAVE\r\n") == 0)
                    {
                        write(fd, "OK\r\n", strlen("OK\r\n"));
                        printf("The client has gone.\n");
                        fflush(stdout);
                        pthread_mutex_lock(&lock);
                        groups->j--;
                        pthread_mutex_unlock(&lock);

                        FD_CLR(fd, &groups->gfds);
                        FD_SET(fd, groups->MainMenufds);
                        // lower the max socket number if needed
                        if (groups->nfds == fd + 1)
                            groups->nfds--;
                    } else if (strcmp(buf, "GETOPENGROUPS\r\n")==0){
                        char message[1000];
                        strcpy(message, "OPENGROUPS");

                        for (int i = 0; i < 32; ++i) {
                            if (groups[i].valid == 1) {
                                strcat(message, "|");
                                strcat(message, groups[i].topic);
                                strcat(message, "|");
                                strcat(message, groups[i].groupname);
                                char str[12];
                                sprintf(str, "%d", groups[i].Lnum);
                                strcat(message, "|");
                                strcat(message, str);
                                sprintf(str, "%d", groups[i].j);
                                strcat(message, "|");
                                strcat(message, str);
                            }
                        }
                        strcat(message, "\r\n");
                        write(fd, message, strlen(message));
                    } else{
                        printf("The client says: %s\n", buf);
                        fflush(stdout);
                        write(fd, "WAIT\r\n", strlen("WAIT\r\n"));
                        //sprintf( buf, "OK\n" );
                    }
                }

            }
        } else {
            printf("P=> %d\n",p);
            break;
        }
    }


    /***********************/
    /*THE QUIZ BEGINS HERE */
    /***********************/
    fseek(file, 0, SEEK_SET);
    for (;;)
    {
        printf("QUESTION TAKING\n");
        fflush(stdout);
        char bufff[2048];
        ///Getting QUEStion
        counter=0;
        if (fgets(bufff, 2048, file) != NULL) {
            strcpy(que, bufff);
            while (1) {
                printf("The Getting question:\n");
                fflush(stdout);
                if (strcmp(bufff, "\n") == 0) counter++;
                if (counter == 1 || fgets(bufff, 2048, file) == NULL) break;
                strcat(que, bufff);
            }
        } else{
            printf("End of quiz");
            fflush(stdout);
            break;
        }
//        printf("The client says: %s\n", buff);
//        fflush(stdout);
        strcpy(mess, "QUES|");

        char str[12];
        sprintf(str, "%d", strlen(que));
        strcat(mess, str);
        strcat(mess, "|");
        strcat(mess, que);
        printf("The next question is: %s\n", mess);
        fflush(stdout);

        //printf("Questions is: %s",que);
        ///Getting Answer
        char ans[2048];
        counter = 0;
        if (fgets(buff, 2048, file) != NULL) {
            //printf("Buffer %s", buff);
            strcpy(ans, buff);
            while (1) {
                if (strcmp(buff, "\n") == 0) counter++;
                if ( counter == 1||fgets(buff, 2048, file) == NULL ) break;
            }
        } else {
            break;
        }
        ans[strlen(ans)-1]='\0';

        ///Sending question to everyone
        for (fd = 3; fd < groups->nfds; fd++) {
            if (FD_ISSET(fd, &groups->gfds)) {
                //printf("YOU see this when client receives question");
                fflush(stdout);
                if (write(fd, mess, strlen(mess))<0){
                    printf("Client did not receive a question\n");
                    FD_CLR(fd, &groups->gfds);
                }
            }
        }

        /// Actual quiz
        int p=0;
        int win =0;
        char winner[200];
        strcpy(winner, "");
        FD_ZERO(&afds);
        timeout.tv_sec = 3;

        time_t init, elapsed;

        init = time(NULL);
        //printf("Hours since January 1, 1970 = %ld\n", init);

        fd_set tfds;
        memcpy((char *) &tfds, (char *) &groups->gfds, sizeof(rfds));

        if (groups->Lnum<=0){
            break;
        }
        while (1) {

            if (p >= groups->Lnum)
                break;

            memcpy((char *) &rfds, (char *) &groups->gfds, sizeof(rfds));

            if (select(groups->nfds, &rfds, (fd_set *) 0, (fd_set *) 0,
                       &timeout) < 0) {
                fprintf(stderr, "server select: %s\n", strerror(errno));
                exit(-1);
            }
            elapsed = time(NULL);
            /*printf("Hours since January 1, 1970 = %ld\n", init);
            printf("time passed %ld\n", elapsed - init);
            fflush(stdout);*/

            if (elapsed - init >= 20) {
                for (fd = 3; fd < groups->nfds; fd++) {
                    if (!FD_ISSET(fd, &rfds) && FD_ISSET(fd, &tfds) && fd != groups->admin) {
                        //KICK-OUT PROCEDURE
                        FD_CLR(fd, &afds);
                        FD_CLR(fd, &groups->gfds);
                        FD_SET(fd, groups->MainMenufds);
                        write(fd, "ENDGROUP\r\n", strlen("ENDGROUP\r\n"));
                        groups->Lnum--;
                    }
                }
                break;
            } else {
                if (FD_ISSET(groups->admin,&rfds)) {
                    if ((cc = read(groups->admin, buf, BUFSIZE)) <= 0) {
                        FD_CLR(groups->admin, &groups->gfds);

                        FD_CLR(groups->admin, &afds);
                        printf("The client has gone 12324.\n");
                        fflush(stdout);
                        (void) close(groups->admin);

                    }
                    buf[cc] = '\0';
                    if (strcmp(buf, "LEAVE\r\n") == 0) {
                        write(groups->admin, "OK\r\n", strlen("OK\r\n"));
                        FD_CLR(groups->admin, &groups->gfds);
                        FD_CLR(groups->admin, &afds);
                        FD_SET(groups->admin, groups->MainMenufds);

                    } else
                        write(groups->admin,"BAD|YOU are not allowed to answer\r\n",strlen("BAD|YOU are not allowed to answer\r\n"));
                }
                for (fd = 3; fd < groups->nfds; fd++) {

                    // check every socket to see if it's in the ready set
                    if (fd != groups->admin && FD_ISSET(fd, &rfds)) {
                        printf("Hello3\n");
                        FD_CLR(fd, &groups->gfds);
                        FD_SET(fd, &afds);
                        if ((cc = read(fd, buf, BUFSIZE)) <= 0) {
                            FD_CLR(fd, &afds);
                            groups->Lnum--;
                            printf("The client has gone 1.\n");
                            fflush(stdout);
                            (void) close(fd);

                        }
                        buf[cc] = '\0';
                        if (strcmp(buf, "LEAVE\r\n") == 0) {
                            write(fd, "OK\r\n", strlen("OK\r\n"));
                            printf("The client has gone 2.\n");
                            fflush(stdout);
                            groups->Lnum--;
                            FD_CLR(fd, &afds);
                            FD_SET(fd, groups->MainMenufds);

                        }
                        buf[cc - 2] = '\0';
                        printf("RECEIVED %s\n", buf);
                        char *token;
                        token = strtok(buf, "|");

                        if (strcmp(token, "ANS") == 0) {
                            printf("THE value of p = %d\n", p);
                            fflush(stdout);
                            p++;
                            token = strtok(NULL, "|");
                            printf("Length of answer: %d and answer is %s and token = %s and size token %d\n ", strlen(ans), ans, token, strlen(token));
                            if (strcmp(token, ans) == 0) {
                                printf("We have right answer for file desc. = %d\n", fd);
                                if (win == 0) {
                                    win = 1;
                                    for (int i = 0; i < lastMember; ++i) {
                                        if (groups->member[i].fd == fd) {
                                            groups->member[i].score++;
                                            groups->member[i].score++;
                                            strcpy(winner, groups->member[i].username);
                                            printf("1THE %s have score %d on position %d file descriptor %d\n",
                                                   groups->member[i].username, groups->member[i].score, i, fd);
                                            fflush(stdout);
                                        }
                                    }
                                } else if (win == 1) {
                                    for (int i = 0; i < lastMember; ++i) {
                                        if (groups->member[i].fd == fd) {
                                            groups->member[i].score++;
                                            printf("2THE %s have score %d on position %d file descriptor %d\n",
                                                   groups->member[i].username, groups->member[i].score, i, fd);
                                            fflush(stdout);
                                        }
                                    }
                                }
                            } else if (strcmp(token, "NOANS") == 0) {

                            } else {
                                for (int i = 0; i < lastMember; ++i) {
                                    if (groups->member[i].fd == fd) {
                                        groups->member[i].score--;
                                        printf("3THE %s have score %d on position %d file descriptor %d\n",
                                               groups->member[i].username, groups->member[i].score, i, fd);
                                        fflush(stdout);
                                    }
                                }
                            }
                            FD_CLR(fd, &tfds);

                        }
                    }

                }

            }
        }
        strcpy(mess, "WIN|");
        printf("winner is : %s\n",winner);
        if (winner != NULL)
            strcat(mess, winner);
        strcat(mess, "\r\n");
        FD_SET(groups->admin, &afds);
        if (groups->nfds<groups->admin){
            groups->nfds = groups->admin+1;
        }
        memcpy((char *) &groups->gfds, (char *) &afds, sizeof(rfds));

        for (fd = 3; fd < groups->nfds; fd++)
        {
            if (FD_ISSET(fd, &afds))
            if(write(fd, mess, strlen(mess))<0){
                /*printf("The client has gone 3.\n");
                fflush(stdout);*/
                (void) close(fd);
                groups->Lnum--;
                FD_CLR(fd, &groups->gfds);
                // lower the max socket number if needed
                if (groups->nfds == fd + 1)
                    groups->nfds--;
            }

        }

    }

    memcpy((char *) &rfds, (char *) &groups->gfds, sizeof(rfds));

    if (select(groups->nfds,  (fd_set *) 0, &rfds, (fd_set *) 0,(struct timeval *)0) < 0) {
        fprintf(stderr, "server select: %s\n", strerror(errno));
        exit(-1);
    }

    char results[255];
    strcpy(results, "RESULT");
    for (int j = 3; j < groups->nfds; j++) {
        if (FD_ISSET(j,&rfds)){
            for (int i = 0; i < lastMember; i++) {
               /* printf("FUCK SMWERE HERE %d %d\n", i, j);
                fflush(stdout);*/
                if (groups->member[i].fd == j ) {
                    /*printf("GOT INSIDE %d %d \n", i, j);
                    fflush(stdout);*/
                    strcat(results, "|");
                    strcat(results, groups->member[i].username);
                    strcat(results, "|");
                    char t[12];
                    sprintf(t, "%d", groups->member[i].score);
                    strcat(results, t);
                    break;
                }
            }
        }

    }
    strcat(results, "| ");
    strcat(results, "\r\n");
    /*printf("Popped outside %s", results);
    fflush(stdout);*/
    for (int i = 3; i<groups->nfds; i++){
        if (FD_ISSET(i,&groups->gfds)) {
            write(i, results, strlen(results));
            FD_SET(i, groups->MainMenufds);
            write(i, "ENDGROUP\r\n", strlen("ENDGROUP\r\n"));
        }
    }
    groups->valid = 0;
    printf("FINISH LINE\n");
    fflush(stdout);





}

/*
**	The server ...
*/