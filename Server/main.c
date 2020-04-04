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

int passivesock( char *service, char *protocol, int qlen, int *rport );
void *groove(void *arg);



int
main( int argc, char *argv[] )
{
    pthread_t threads[1020];
    pthread_attr_t attr;
	char			buf[BUFSIZE];
	char			*service;
	struct sockaddr_in	fsin;
	int			msock;
	int			ssock;
	fd_set			rfds;
	fd_set			afds;
	int			alen;
	int			fd;
	int			nfds;
	int			rport = 0;
	int			cc;



	switch (argc)
	{
		case	1:
			// No args? let the OS choose a port and tell the user
			rport = 1;
			break;
		case	2:
			// User provides a port? then use it
			service = argv[1];
			break;
		default:
			fprintf( stderr, "usage: server [port]\n" );
			exit(-1);
	}

	msock = passivesock( service, "tcp", QLEN, &rport );
	if (rport)
	{
		//	Tell the user the selected port
		printf( "server: port %d\n", rport );
		fflush( stdout );
	}
    for (int j = 0; j < 32; ++j) {
        groups[j].valid=0;
    }


	// Set the max file descriptor being monitored
	nfds = msock+1;

	FD_ZERO(&afds);
	FD_SET( msock, &afds );
    struct timeval timeout;
    timeout.tv_sec = 4;
    timeout.tv_usec = 0;

	for (;;)
	{
		// Reset the file descriptors you are interested in
		memcpy((char *)&rfds, (char *)&afds, sizeof(rfds));

		// Only waiting for sockets who are ready to read
		//  - this also includes the close event
		if (select(nfds, &rfds, (fd_set *)0, (fd_set *)0,
                   &timeout) < 0)
		{
			fprintf( stderr, "server select: %s\n", strerror(errno) );
			exit(-1);
		}

        // Since we've reached here it means one or more of our sockets has something
		// that is ready to read

		// The main socket is ready - it means a new client has arrived
		if (FD_ISSET( msock, &rfds))
		{
			int	ssock;

			// we can call accept with no fear of blocking
			alen = sizeof(fsin);
			ssock = accept( msock, (struct sockaddr *)&fsin, &alen );
			if (ssock < 0)
			{
				fprintf( stderr, "accept: %s\n", strerror(errno) );
				exit(-1);
			}
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
            write(ssock, message, strlen(message));
			/* start listening to this guy */
			FD_SET( ssock, &afds );

			// increase the maximum
			if ( ssock+1 > nfds )
				nfds = ssock+1;
		}

		/*	Handle the participants requests  */
		for ( fd = 3; fd < nfds; fd++ )
		{
			// check every socket to see if it's in the ready set
			if (fd != msock && FD_ISSET(fd, &rfds))
			{
				// read without blocking because data is there
				if ( (cc = read( fd, buf, BUFSIZE )) <= 0 )
				{
					printf( "The client has gone.\n" );
					(void) close(fd);
					FD_CLR( fd, &afds );
					// lower the max socket number if needed
					if ( nfds == fd+1 )
						nfds--;

				}
				else
				{
                    buf[cc-2] = '\0';
                    char *token;
                    token = strtok(buf, "|");
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
                        write(fd, message, strlen(message));
                    }
                    else if (strcmp(token, "GROUP")==0){

                        /*****************************/
                        /*THIS SOCKET TRIED TO CREATE NEW GROUP  */
                        /*****************************/
                        int k=32;
                        for (int i = 0; i < 32; i++) {
                            if (groups[i].valid==0){
                                k=i;
                                break;
                            }

                        }
                        if (k==32){
                            write(fd,"BAD|Too much groups\r\n", strlen("BAD|Too much groups\r\n"));
                            continue;
                        }
                        token = strtok(NULL, "|");
                        strcpy(groups[k].topic, token);
                        token = strtok(NULL, "|");
                        player user;
                        strcpy(user.groupname, token);
                        strcpy(groups[k].groupname, token);

                        int groupnameAvailable = 0;
                        for (int i = 0; i < 32; i++) {
                            if (strcmp(groups[i].groupname, token)==0 && groups[i].valid==1){
                                groupnameAvailable=1;
                                break;
                            }

                        }
                        if (groupnameAvailable==1){
                            write(fd,"BAD|groupname taken\r\n", strlen("BAD|groupname taken\r\n"));
                            printf("groupname taken\n");
                            continue;
                        }

                        token = strtok(NULL, "|");
                        groups[k].Lnum = atoi(token);
                        groups[k].valid = 1;
                        groups[k].j = 0;
                        groups[k].MainMenufds = &afds;
                        FD_ZERO(&groups[k].gfds);
                        FD_CLR(fd, &afds);

                        if (pthread_create(&threads[k], NULL, groove, &groups[k]) < 0) {
                            printf("Mission Failed\n");
                        } else {
                            groups[k].admin = fd;
                            printf("Client connected\n");
                        }
                    } else if(strcmp(token, "JOIN")==0){
                        /*****************************/
                        /*THIS SOCKET TRIED TO JOIN  */
                        /*****************************/
                        int k = 32;
                        token = strtok(NULL, "|");
                        for (int i=0; i < 32; i++){
                            if (strcmp(token, groups[i].groupname)==0 && groups[i].valid == 1){
                                k=i;
                                break;
                            }
                        }
                        if (k==32){
                            write(fd, "BAD|no such group\r\n", strlen("BAD|no such group\r\n"));
                            continue;
                        }
                        if (groups[k].j >= groups[k].Lnum){
                            write(fd, "BAD|group is full\r\n", strlen("BAD|group is full\r\n"));
                            continue;
                        }
                        pthread_mutex_lock(&lock);
                        strcpy(groups[k].member[groups[k].j].groupname, token);
                        FD_SET(fd, &groups[k].gfds);
                        FD_CLR(fd, &afds);
                        token = strtok(NULL, "|");
                        if (groups[k].nfds<=fd)
                            groups[k].nfds = fd+1;

                        groups[k].member[groups[k].j].fd=fd;
                        groups[k].member[groups[k].j].score=0;
                        strcpy(groups[k].member[groups[k].j].username, token);
                        printf("THIS GUY JOINED: %s\n",groups[k].member[groups[k].j].username);
                        fflush(stdout);
                        write(fd, "OK\r\n", strlen("OK\r\n"));
                        printf("Client: %d %d joined %s\n",fd, groups[k].nfds, groups[k].groupname);
                        fflush(stdout);
                        groups[k].j++;
                        pthread_mutex_unlock(&lock);

                    }

					printf( "The client says in lobby: %s\n", buf );
					//sprintf( buf, "OK\n" );
                    //write( fd, buf, strlen(buf) );
				}
			}

		}
	}
}

