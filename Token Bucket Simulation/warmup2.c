#include <stdio.h>
#include <pthread.h>
#include <unistd.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <sys/time.h>
#include <signal.h>
#include <math.h>
#include <errno.h>
#include "my402list.h"
#include "cs402.h"


My402List Q1;
My402List Q2;

//====default values=====//

double lambda=(1/0.5)*1000000;
double mu=(1/0.35)*1000000;
double r=(1/1.5)*1000000;
int B=10;
int P=3;
int num=20;

//========================//

pthread_t arrival_id;
pthread_t token_id;
pthread_t server_id;

struct timeval start, end;
double time_in_Q1,time_in_Q2, display;
int no_of_token;
int i,j;
float l,m;

sigset_t sig;
struct sigaction action;

My402ListElem* head=NULL;
My402ListElem* head2=NULL;

//----------for stats--------
double intArrivalTime=0;
int totalPacketsDropped=0;
double totalTimeinQ1=0;
double totalTimeinQ2=0;
int totalPacketsEntered=0;
int totalPacketsMove=0;
int totalTokens=0;
int tokensDropped=0;
int totalPacketsServiced=0;
double totalTimeinSystem=0;
double totalServicedTime=0;
double totalTimeinServer=0;
//-----------------------------

double avgtimeinSystem=0,variance=0,standard_dev=0;
double serviceTime=0;

int token_counter=0, server_counter=0;

double array[1000];
int k=0;

//-----------------------------

int p_count=1,token_in_bucket=0,servercount=1,tcount=1,s_counter=1, flag=0;
char *fname=NULL;

FILE *fp;

pthread_mutex_t m_tex=PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t server=PTHREAD_COND_INITIALIZER;

typedef struct packet_data{
    
    int P;
    double lambda;
    double mu;
    struct timeval sysEnter;
    struct timeval Q1enter;
    struct timeval Q1left;
    struct timeval Q2enter;
    struct timeval Q2left;
    struct timeval sysleft;
    struct timeval serverEnter;
    struct timeval serverExit;
    
}packet;

void terminate(int sig)
{
    
        if(!My402ListEmpty(&Q1))
       {
           My402ListUnlinkAll(&Q1);
       }
       if(!My402ListEmpty(&Q2))
       {
           My402ListUnlinkAll(&Q2);
       }
    pthread_cancel(arrival_id);
    pthread_cancel(token_id);
    flag=1;
}


double elapsedTime(struct timeval t1,struct timeval t2)
{
    double elapsedtime;
    elapsedtime =(double)((t2.tv_sec - t1.tv_sec) * 1000000); 
    elapsedtime +=(double)((t2.tv_usec - t1.tv_usec));
    return elapsedtime;
}


void* arrival_Thread(void* fp)
{
    action.sa_handler=terminate;
    sigaction(SIGINT, &action, NULL);
    pthread_sigmask(SIG_UNBLOCK, &sig, NULL);
    
    fp=(FILE*)fp;
    
    struct timeval cur,begin, arr_diff;
    double timeForArrival;
    
    begin=start;
    
    char buffer[1026];
    
    if(fname != NULL)
    {
        while(fgets(buffer,sizeof(buffer),fp) != NULL)
        {
            
            buffer[1026]='\0';
            if(strlen(buffer)>1024)
            {
                fprintf(stdout,"Invalid input\n");
                exit(0);
            }
            
            sscanf(buffer,"%f %d %f",&l,&P,&m);
            
            if(l<=0)
            {
                fprintf(stdout," ERROR: lambda should be positive real no\n");
                exit(0);
            }
            else if(l>10000)
            {
                l=10000;
            }
            
            if(P < 1 || P > 2147483647)
            {
                fprintf(stdout,"ERROR: invalid input for P no\n");
                exit(0);
            }
            
            if(m <= 0)
            {
                fprintf(stdout,"ERROR: mu should be a positive real no\n");
                exit(0);
            }
            else if(m > 10000)
            {
                mu = 10000;
            }
            
            lambda=(double)l*1000;
            mu = (double)m*1000;
            
            packet* object=(packet*)malloc(sizeof(packet));
            
            //----creating packet object
    
            object->P=P;
            object->lambda=lambda;
            object->mu=mu;
    
            
            gettimeofday(&cur,NULL);
            
            double time_diff=elapsedTime(begin,cur);        //----if lambda is not fulfilled
            
            if(lambda > time_diff)
            {
                double sleep=lambda-time_diff;
                
                usleep(sleep);
            }
            else{
                usleep(0);
            }
            
            
            
            gettimeofday(&arr_diff,NULL);   //for calculating inter arrival of each packet
            
            timeForArrival=elapsedTime(begin,arr_diff);     //storing inter arr time
            
            intArrivalTime += timeForArrival;   //----for stats...1...
            
            gettimeofday(&begin,NULL);  //packet entered system
            
            
            
            gettimeofday(&(object->sysEnter),NULL);
            
           
            double arr_time= elapsedTime(start,begin);// time packet arrived
            fprintf(stdout,"%012.3fms: p%d arrives, needs %d tokens, inter-arrival time = %.03fms\n",(arr_time/1000),p_count,P,(timeForArrival/1000));
            
            totalPacketsEntered++;
            
            pthread_mutex_lock(&m_tex);
            
            if(P > B)
             {
               
                 fprintf(stdout,"%012.3fms: p%d arrives, needs %d tokens,dropped\n",(arr_time/1000),p_count,P);
                 totalPacketsDropped++;
                 token_counter--;
                 server_counter--;
                 
                 if(totalPacketsDropped == num)
                 {
                     pthread_cond_broadcast(&server);
                 }
                 
                 
             }
            else
            {
                
                My402ListAppend(&Q1,(void*)object);

                gettimeofday(&(object->Q1enter),NULL);

                
                 display=elapsedTime(start,(object->Q1enter));
                
                fprintf(stdout,"%012.3fms: p%d enters Q1\n",(display/1000),p_count);
                
                
                
            }
            
            if(!My402ListEmpty(&Q1))
            {
                head= My402ListFirst(&Q1);
                packet* info=(packet*)(head->obj);
                no_of_token=info->P;
                if(token_in_bucket >= no_of_token)
                {

                    gettimeofday(&(info->Q1left),NULL);
                    
                    token_in_bucket -= no_of_token;
                    My402ListUnlink(&Q1,head);
                    totalPacketsMove++;     //keeping track of packets moved...
                    token_counter--;
                   
                    
                    time_in_Q1=elapsedTime((info->Q1enter),(info->Q1left));
                    
                    totalTimeinQ1 += time_in_Q1;        //for stats
                    
                    display=elapsedTime(start,(info->Q1left));      //using the same display variable
                    fprintf(stdout,"%012.3fms: p%d leaves Q1,time in Q1=%.03fms,token bucket now has %d tokens\n",(display/1000),servercount,(time_in_Q1/1000),token_in_bucket);
                    
                    if(My402ListEmpty(&Q2))     //if Q2 is empty add and broadcadst
                    {
                        My402ListAppend(&Q2,info);
                        pthread_cond_broadcast(&server);
                    }
                    else
                    {
                        My402ListAppend(&Q2,info);  //else simply add
                    }

                    
                    gettimeofday(&(info->Q2enter),NULL);
                    display=elapsedTime(start,(info->Q2enter));

                    
                    fprintf(stdout,"%012.3fms: p%d enters Q2\n",(display/1000),servercount);
                    servercount++;
                    pthread_cond_broadcast(&server);
                    
                }
            }
            p_count++;
            pthread_mutex_unlock(&m_tex);
            if(flag==1)
            {
                break;
            }
        }
    }
    else
    {
        for(p_count=1;p_count<=num;p_count++)
        {
            packet* object=(packet*)malloc(sizeof(packet));
            //----creating packet object
            
            object->P=P;
            object->lambda=lambda;
            object->mu=mu;
            
            
            gettimeofday(&cur,NULL);
            
            double time_diff=elapsedTime(begin,cur);        //----if lambda is not fulfilled
            
            if(lambda > time_diff)
            {
                double sleep=lambda-time_diff;
                
                usleep(sleep);
            }
            else{
                usleep(0);
            }
            
            
            
            gettimeofday(&arr_diff,NULL);   //for calculating inter arrival of each packet
            
            timeForArrival=elapsedTime(begin,arr_diff);     //storing inter arr time
            
            intArrivalTime += timeForArrival;
            
            gettimeofday(&begin,NULL);  //packet entered system
            
            gettimeofday(&(object->sysEnter),NULL);
            
            double arr_time= elapsedTime(start,begin);// time packet arrived
            
        
            
            fprintf(stdout,"%012.3fms: p%d arrives, needs %d tokens, inter-arrival time = %.03fms\n",(arr_time/1000),p_count,P,(timeForArrival/1000));
            totalPacketsEntered++;
            
            pthread_mutex_lock(&m_tex);
            if(P > B)
            {
                
                fprintf(stdout,"%012.3fms: p%d arrives, needs %d tokens,dropped\n",(arr_time/1000),p_count,P);
                totalPacketsDropped++;
                token_counter--;
                server_counter--;
            
                if(totalPacketsDropped == num)
                {
                    pthread_cond_broadcast(&server);
                }
                
            }
            else
            {
                
                My402ListAppend(&Q1,(void*)object);
                gettimeofday(&(object->Q1enter),NULL);
                 display=elapsedTime(start,(object->Q1enter));
                
                fprintf(stdout,"%012.3fms: p%d enters Q1\n",(display/1000),p_count);
                
                
                
            }
            
            if(!My402ListEmpty(&Q1))
            {
                head= My402ListFirst(&Q1);
                packet* info=(packet*)(head->obj);
                no_of_token=info->P;
                if(token_in_bucket >= no_of_token)
                {
                    gettimeofday(&(info->Q1left),NULL);
                    
                    token_in_bucket -= no_of_token;
                    My402ListUnlink(&Q1,head);
                    totalPacketsMove++;     //keeping track of packets moved...
                    token_counter--;
                    
                    
                     time_in_Q1=elapsedTime((info->Q1enter),(info->Q1left));
                    
                    totalTimeinQ1 += time_in_Q1;        //for stats
                    
                    display=elapsedTime(start,(info->Q1left));      //using the same display variable
                    fprintf(stdout,"%012.3fms: p%d leaves Q1,time in Q1=%.03fms,token bucket now has %d tokens\n",(display/1000),servercount,(time_in_Q1/1000),token_in_bucket);
                    
                    if(My402ListEmpty(&Q2))     //if Q2 is empty add and broadcadst
                    {
                        My402ListAppend(&Q2,info);
                        pthread_cond_broadcast(&server);
                    }
                    else
                    {
                        My402ListAppend(&Q2,info);  //else simply add
                    }
                    gettimeofday(&(info->Q2enter),NULL);
                    display=elapsedTime(start,(info->Q2enter));
                    fprintf(stdout,"%012.3fms: p%d enters Q2\n",(display/1000),servercount);
                    servercount++;
                    pthread_cond_broadcast(&server);
                    
                }
            }
            pthread_mutex_unlock(&m_tex);
            if(flag==1)
            {
                break;
            }

        }
    }
    pthread_exit((void *)1);
    return 0;
}


void* bucket_Thread()
{
    action.sa_handler=terminate;
    sigaction(SIGINT, &action, NULL);
    pthread_sigmask(SIG_UNBLOCK, &sig, NULL);
    
    struct timeval now,st;
    st=start;
    double sleeptime,sleepp;
    double present_time;
    
    while(token_counter != 0)
    {
        gettimeofday(&now,NULL);
    
        sleeptime=elapsedTime(st,now);
        if(r > sleeptime )
        {
            sleepp=r-sleeptime;
        }
        else
        {
            sleepp=0;
        }
        usleep(sleepp);
        
       
        gettimeofday(&st,NULL);
        totalTokens++;
        present_time=elapsedTime(start,st);
        
        if(token_in_bucket < B)
        {
            token_in_bucket++;
            fprintf(stdout,"%012.3fms: token t%d arrives,token bucket has now %d token(s)\n",(present_time/1000),tcount,token_in_bucket);
        }
        else
        {
            tokensDropped++;
            fprintf(stdout,"%012.3fms: token t%d arrives,dropped\n",(present_time/1000),tcount);
        }
        tcount++;
         pthread_mutex_lock(&m_tex);
        
        if(!My402ListEmpty(&Q1))
        {
            head= My402ListFirst(&Q1);
            packet* info=(packet*)(head->obj);
            no_of_token=info->P;
            if(token_in_bucket >= no_of_token)
            {
                
                gettimeofday(&(info->Q1left),NULL);
                
                token_in_bucket -= no_of_token;
                My402ListUnlink(&Q1,head);
                totalPacketsMove++;     //keeping track of packets moved...
                token_counter--;
                time_in_Q1=elapsedTime((info->Q1enter),(info->Q1left));
                
                totalTimeinQ1 += time_in_Q1;        //for stats
                
                display=elapsedTime(start,(info->Q1left));      //using the same display variable
                fprintf(stdout,"%012.3fms: p%d leaves Q1,time in Q1=%.03fms,token bucket now has %d tokens\n",(display/1000),servercount,(time_in_Q1/1000),token_in_bucket);
                
                if(My402ListEmpty(&Q2))     //if Q2 is empty add and broadcadst
                {
                    My402ListAppend(&Q2,info);
                    pthread_cond_broadcast(&server);
                }
                else
                {
                    My402ListAppend(&Q2,info);  //else simply add
                }
                gettimeofday(&(info->Q2enter),NULL);
                display=elapsedTime(start,(info->Q2enter));
                fprintf(stdout,"%012.3fms: p%d enters Q2\n",(display/1000),servercount);
                servercount++;
                
            }

        }
        pthread_mutex_unlock(&m_tex);
        if(totalPacketsDropped==num)
        {
            break;
        }
        if(flag==1)
        {
            break;
        }
    }
         pthread_cond_broadcast(&server);
        pthread_exit((void *)1);
        return 0;
        

}


void* server_Thread()
{
    
    double cur_time,actualTime,mmu;
    struct timeval wake;
    while(server_counter != 0)
    {
        pthread_mutex_lock(&m_tex);
        if(My402ListEmpty(&Q2))
        {
            pthread_cond_wait(&server,&m_tex);  //waits until signal is broadcast
            if(totalPacketsEntered==totalPacketsDropped || flag==1)
            {
                pthread_mutex_unlock(&m_tex);
                break;
            }
        }
        
        gettimeofday(&wake,NULL);
        head2=My402ListFirst(&Q2);
        packet* info2=(packet*)head2->obj;
        mmu= info2->mu;
        
        My402ListUnlink(&Q2,head2);
        
        gettimeofday(&(info2->Q2left),NULL);
        
        time_in_Q2=elapsedTime((info2->Q2enter),(info2->Q2left));
        totalTimeinQ2 += time_in_Q2;
        cur_time=elapsedTime(start,(info2->Q2left));
        
        fprintf(stdout,"%012.3fms: p%d begin service at S, time in Q2=%.03fms\n",(cur_time/1000),s_counter,(time_in_Q2/1000));

        gettimeofday(&(info2->serverEnter),NULL);
        
        pthread_mutex_unlock(&m_tex);
        usleep(mmu);
        
        pthread_mutex_lock(&m_tex);
        
        totalPacketsServiced++;
        server_counter--;
      
        
        gettimeofday(&(info2->serverExit),NULL);        //exit time from server
        
        totalTimeinServer += elapsedTime((info2->serverEnter),(info2->serverExit));
        serviceTime = elapsedTime((info2->serverEnter),(info2->serverExit));
        
        gettimeofday(&(info2->sysleft),NULL);
        
        double time_in_system=elapsedTime(info2->sysEnter,info2->sysleft);
        
        totalTimeinSystem += time_in_system;
        array[k] = time_in_system;
        k++;
        
        totalServicedTime += serviceTime;   
        
        actualTime=elapsedTime(start,(info2->sysleft));
        
        fprintf(stdout,"%012.3fms: p%d departs from S, Service time=%.03fms,\n\t\ttime in System=%.03fms\n",(actualTime/1000),s_counter,(serviceTime/1000),(time_in_system/1000));
        
        s_counter++;
        
        
        pthread_mutex_unlock(&m_tex);
        if(flag==1)
        {
            break;
        }
        
    }
    pthread_exit((void *)1);
    return 0;
    
    
    
}



void printError()
{
    fprintf(stdout,"Command line argument not valid\n");
    fprintf(stdout,"Usage: ./warmup2 [-n n] [-lambda lambda] [-r r] [-B B] [-P P] [-mu mu] [-t fname]\n");
    fprintf(stdout,"program exiting\n");
}

void parseArgument(int argc, char *argv[])
{
    if(argc<1)  //if no argument is provided
    {
        fprintf(stdout,"ERROR: INVALID no of arguments\n");
        exit(0);
    }
    
    
    for( i=1;i<argc;i+=2)
    {
        if(argv[i][0] != '-')//-----if any argument is not preceded with '-' sign-------//
        {
            printError();
            exit(1);
        }
    }
    
    for( j=1;j<argc;j++)
    {
        if(strcmp(argv[j],"-lambda")==0)
        {
            j++;
            lambda=atof(argv[j]);
            
            if(lambda <= 0)
            {
                fprintf(stdout,"ERROR: lambda should be a positive real no\n");
                exit(0);
            }
            
            if((1/lambda)>10)
            {
                lambda=0.1;
            }
            lambda=(1/lambda)*1000000;
            
        }
        
        else if(strcmp(argv[j],"-r")==0)     // ----checking for the r value----
        {
            j++;
            r=atof(argv[j]);
            
            if(r <= 0)
            {
                fprintf(stdout,"ERROR: r should be a positive real no\n");
                exit(0);
            }
            
            if((1/r)>10)
            {
                r=0.1;
            }
            
            r=(1/r)*1000000;    //---coverting sec to microsec---
        }
        
        else if(strcmp(argv[j],"-mu")==0)     // ----checking for the r value----
        {
            j++;
            mu=atof(argv[j]);
            
            if(mu <= 0)
            {
                fprintf(stdout,"ERROR: r should be a positive real no\n");
                exit(0);
            }
            
            if((1/mu)>10)
            {
                mu=0.1;
            }
            
            mu=(1/mu)*1000000;    //---coverting sec to microsec---
        }
        
        else if(strcmp(argv[j],"-B")==0)
        {
            j++;
            B=atoi(argv[j]);
            if(B < 1 || B > 2147483647)
            {
                fprintf(stdout,"ERROR: invalid input\n");
                exit(0);
            }
            
        }
        
        else if(strcmp(argv[j],"-P")==0)
        {
            j++;
            P=atoi(argv[j]);
            if(P < 1 || P > 2147483647)
            {
                fprintf(stdout,"ERROR: invalid input\n");
                exit(0);
            }
            
        }
        else if(strcmp(argv[j],"-n")==0)
        {
            j++;
            num=atoi(argv[j]);
            if(num < 1 || num > 2147483647)
            {
                fprintf(stdout,"ERROR: invalid input\n");
                exit(0);
            }
            
        }
        
        else if(strcmp(argv[j],"-t")==0)//-----if file is specified-----//
        {
            j++;
            fname=(char*)malloc(100*sizeof(char));
            fname=argv[j];
            if(fopen(fname,"r")==NULL || fopen(fname,"r")==0)
            {
                fprintf(stdout,"ERROR opening file.QUITTING\n ");
                exit(0);
            }
            fp=fopen(fname,"r");
            char buf[1024];
            buf[1024]='\0';
            if(fgets(buf,sizeof(buf),fp) != NULL)
            {
                num=atoi(buf);
            }
        }
        else
        {
            printError();
            exit(1);
        }
        
    }

}


int main(int argc,char *argv[])
{
    
    memset(&Q1,0,sizeof(My402List));
    memset(&Q2,0,sizeof(My402List));
    
    My402ListInit(&Q1);
    My402ListInit(&Q2);
    
    
    
    gettimeofday(&start,NULL);
    
    parseArgument(argc,argv);
    
    double s_time=elapsedTime(start,start); //----to get start---
    
    printf("Emulation Parameters:\n ");
    if(fname == NULL)
    {
        printf(" lambda = %.6g\n",(1/(lambda/1000000)));
        printf("  mu = %.6g\n",(1/(mu/1000000)));
    }
    printf("  r = %.6g\n",(1/(r/1000000)));
    printf("  B = %d\n",B);
    if(fname == NULL)
    {
        printf("  P = %d\n",P);
    }
    if(fname == NULL)
    {
        printf("  number to arrive = %d\n",num);
    }
    if(fname != NULL)
    {
        printf("  tsfile = %s\n\n",fname);
    }
    else
    {
        printf("\n");
    }
    
        token_counter= num;
        server_counter=num;

    
    fprintf(stdout,"%012.03Fms: emulation begins\n",s_time/1000); // print first line
    
    
    sigemptyset(&sig);
    sigaddset(&sig,SIGINT);
    pthread_sigmask(SIG_BLOCK, &sig, NULL);
    //==========Creating thread============================//
    
    if(num!=0)
    {
        int err_arrival = pthread_create(&arrival_id,0,arrival_Thread,(void*)fp);
        if(err_arrival != 0)
        {
            fprintf(stderr,"ERROR: arrival thread::%s QUITTING\n",strerror(err_arrival));
            exit(1);
        }
        
        int err_bucket = pthread_create(&token_id,0,bucket_Thread,NULL);
        if(err_bucket != 0)
        {
            fprintf(stderr,"ERROR:token bucket thread::%s QUTTING\n ",strerror(err_bucket));
            exit(1);
        }
        
        int err_server = pthread_create(&server_id,0,server_Thread,NULL);
        if(err_server != 0)
        {
            fprintf(stderr,"ERROR: server thread::%s QUITTING\n",strerror(err_server));
            exit(1);
        }
        
        
        pthread_join(arrival_id,0);
        pthread_join(token_id,0);
        pthread_join(server_id,0);
    }
    
    gettimeofday(&end,NULL);
    double emulationTime=elapsedTime(start,end);
    
    //============================================================//
    
    //===========Statistics================//
    
    fprintf(stdout,"\nStatistics:\n");
    
    double square;
    
    if(totalPacketsEntered == 0)
    {
        fprintf(stdout,"\taverage packet inter-arrival time = (not available because no packet was entered during the entire emulation)\n");
    }
    else
    {
        fprintf(stdout,"\taverage packet inter-arrival time = %.6gs\n",(intArrivalTime/(totalPacketsEntered*1000000)));
    }
    
    
    
    if(totalPacketsServiced == 0)
    {
        fprintf(stdout,"\taverage packet service time = (not available because no packet was served during the entire emulation)\n\n");
    }
    else
    {
        fprintf(stdout,"\taverage packet service time = %.6gs\n\n",(totalServicedTime/(totalPacketsServiced*1000000)));
    }
    
    fprintf(stdout,"\taverage number of packets in Q1 = %.6g\n",(totalTimeinQ1/emulationTime));
    fprintf(stdout,"\taverage number of packets in Q2 = %.6g\n",(totalTimeinQ2/emulationTime));
    fprintf(stdout,"\taverage number of packets at S = %.6g\n\n",(totalTimeinServer/emulationTime));
    
    if(totalPacketsServiced != 0)
    {
        avgtimeinSystem = totalTimeinSystem/totalPacketsServiced;
        fprintf(stdout,"\taverage time a packet spent in a system = %.6gs\n",(avgtimeinSystem/1000000));
    }
    else
    {
        fprintf(stdout,"\taverage time a packet spent in a system = (not available because no packet was served during the entire emulation)\n");
    }
    
    
    if(avgtimeinSystem != 0)
    {
        for(k=0; k<totalPacketsServiced; k++)
        {
            square += (array[k]*array[k]);
        }
        variance=(((square)/totalPacketsServiced)-(avgtimeinSystem*avgtimeinSystem));
        standard_dev=sqrt(variance);
        fprintf(stdout,"\tstandard deviation for time spent in system = %.6gs\n\n",(standard_dev/1000000));
    }
    else
    {
        fprintf(stdout,"\tstandard deviation for time spent in system = 'NA': average time in system is zero\n");
    }
    
    
    
    
    
    if(totalTokens == 0)
    {
        fprintf(stdout,"\ttoken drop probability = (not available because no token was generated during the entire emulation)\n");
    }
    else
    {
        printf("\ttoken drop probability = %.6g\n",(double)((double)tokensDropped/(double)totalTokens));
    }
    
    if(totalPacketsEntered == 0)
    {
        fprintf(stdout,"\tpacket drop probability = (not available because no packet was entered during the entire emulation)\n");
    }
    else
    {
        fprintf(stdout,"\tpacket drop probability = %.6g\n",(double)((double)totalPacketsDropped/(double)totalPacketsEntered));
    }
    return 0;
    
}
