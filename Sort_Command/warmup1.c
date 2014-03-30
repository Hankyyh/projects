#include "cs402.h"
#include "my402list.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <locale.h>
#include <monetary.h>
#include <errno.h>
#include<sys/stat.h>
#include<unistd.h>
#include<ctype.h>



typedef struct data_structure{
    char type;
    int time;
    int tran_amt;
    char *desc;
}Data;


void sort(My402List* final_list)        
{
    My402ListElem *first_ptr, *second_ptr, *innerelemminus;
    Data *inner, *innerminus, *temp;
    int i=0;
    
    for(first_ptr=My402ListFirst(final_list);first_ptr!=NULL;first_ptr=My402ListNext(final_list,first_ptr))
    {
        i=0;
        for(second_ptr=My402ListLast(final_list);second_ptr!=My402ListFirst(final_list);second_ptr=My402ListPrev(final_list,second_ptr))      
        {
            inner=(Data*)second_ptr->obj;
            innerelemminus= My402ListPrev(final_list,second_ptr);
            innerminus=(Data*)innerelemminus->obj;
            
                       
            if(inner->time==innerminus->time)
            {
                printf("ERROR:Two transaction with same timestamp\n");
                exit(0);
            }
            if(innerminus->time > inner->time)
            {
                temp = (Data*) innerelemminus->obj;
                innerelemminus->obj = second_ptr->obj;
                second_ptr->obj = (void*)temp;
                i=1;
                
            }
            
        }
        
    }
  
    
}

int amountChk(char* transactionamount)
{
    
   
    char *dollar, *cents;
    int amount;
    
    int flag = 0;
    
    char *start=transactionamount;
    char *tab=strchr(start,'.');
    if(tab!=NULL)
    {
        *tab++='\0';
        flag++;
    }
    dollar=start;
    
    cents=tab;
    flag++;
    
    if(flag!=2)
    {
        printf("ERROR: Invalid transaction amount\n");
        exit(0);
    }
    
    
    if(strlen(dollar)>7)
    {
        fprintf(stderr,"ERROR: invalid transaction amount.\n");
        exit(0);
        
    }
    
   
    if(strlen(cents)>2)
    {
        fprintf(stderr,"ERROR:invalid transaction amount..\n");
        exit(0);
    }
    
    int dollr=atoi(dollar);
    int cent=atoi(cents);
    
    amount=(dollr*100)+cent;
    
    return amount;
}



int Read_input(FILE* fp,My402List* my402list)
{
    
    char buf[1026];     //creating buffer to input the lines//
    int tab_count=0;
    int count=0;
    while (fgets(buf,sizeof(buf),fp)!=NULL)
    {
        buf[1026]='\0';
        char trantype;
        char *transactiontime, *transactionamount, *transactiondetail;
        transactiontime=NULL;
        transactionamount=NULL;
        transactiondetail=NULL;
        
        if(strlen(buf)>1024)
        {
            fprintf(stderr,"ERROR:quitting because input line is > than 1024\n");    
            exit(0);
        }
        
        count=0;
        tab_count=0;
        
        //Credited to Professor Bill Cheng from the notes given in the lecture.        
        
        char *start_ptr = buf;
        char *tab_ptr = strchr(start_ptr, '\t');
        if(tab_ptr=='\0')
        {
            printf("ERROR:Insufficient data. (QUITTING)\n");
            exit(1);
        }
        if (tab_ptr != NULL)
        {
            *tab_ptr++ = '\0';
            count++;
            tab_count++;
        }
        trantype=*start_ptr;
        
        
        
        if(trantype !='+' && trantype !='-')
        {
            
            printf("ERROR:Invalid input type. Exiting\n");
            exit(0);
            
        }
        
        //=======inputting date========//
        
        start_ptr=tab_ptr;
        tab_ptr = strchr(start_ptr, '\t');
        if(tab_ptr=='\0')
        {
            printf("error: Insufficient data.exiting\n");
            exit(1);
        }
        if (tab_ptr != NULL)
        {
            *tab_ptr++ = '\0';
            count++;
            tab_count++;
        }
        
        transactiontime=start_ptr;
        
        //=========inputting amount============//

        start_ptr=tab_ptr;
        tab_ptr = strchr(start_ptr, '\t');
        if(tab_ptr=='\0')
        {
            printf("Insufficient data. (Program exiting)\n");
            exit(1);
        }
        
        if (tab_ptr != NULL)
        {
            *tab_ptr++ = '\0';
            count++;
            tab_count++;
        }
        transactionamount=start_ptr;
        
        
        //======================================//
        
        //=======inputting description==========//
        
        
        
        transactiondetail=tab_ptr;
        count++;
        if(count!=4)
        {
            printf("ERROR: INVALID INPUT\n");
        }
        if(tab_ptr=='\0')
        {
            printf("ERROR: Malformed input\n");
            exit(0);
        }
        start_ptr=tab_ptr;
        tab_ptr = strchr(start_ptr, '\t');
        if(tab_ptr!=NULL)
        {
            tab_count++;
        }
        if(tab_count!=3)
        {
            printf("ERROR: Invalid input. More than 3 tabs.Quitting\n");
            exit(0);
        }
        
        
        int current_time=(int)time(NULL);
       
        
        
        if(strlen(transactiontime)>10)
        {
            printf("invalid input..\n");
            exit(0);
        }
        else if(atoi(transactiontime) == 2147483647)   
        {
            fprintf(stderr, "Invalid Transaction Time !! Quitting\n");
            exit(0);
        }
        else if(atoi(transactiontime)>current_time)
        {
            printf("ERROR: Invalid time\n");
            exit(0);
        }
        int tran_time=atoi(transactiontime);
        /*=========================================================*/
        
        
       
     
        int amount=amountChk(transactionamount);

        if(strlen(transactiondetail)==0)
        {
            fprintf(stderr,"invalid transaction desc\n");
            exit(0);
        }
       
        
        /*===========================================================*/
        
        
        
        Data *data =(Data*) malloc(sizeof(Data));
        data->type=trantype;
        data->time=tran_time;
        data->tran_amt=amount;
        data->desc=(char*)malloc(strlen(transactiondetail)+1);
        strcpy(data->desc,transactiondetail);
        
        
        My402ListAppend(my402list,(void*)data);
        
       
        
    }
       return TRUE;        
}

/*********************TIME print**********************************/

void printtime(Data* list)
{
    char* timeStr;
    time_t timeStamp = (list->time);      
    struct tm *timeStructure;
    
    timeStr = (char*) malloc (25);
    
    timeStructure = localtime ( &timeStamp );
    
    strftime(timeStr,25,"%a %b %e %Y",timeStructure);
    
    fprintf(stdout,"| %s ",timeStr);
    
    free(timeStr);
}

/**********************DESCRIPTION PRINT****************************/

void printdescription(Data* list)
{
    char* descp;
    descp = (char*) malloc (25);
    char* length=strchr(list->desc,'\n');
    if(length!=NULL)
    {
        *length='\0';
    }
    
    if(strlen(list->desc) > 24)
    {
        strncpy(descp, list->desc, 24);
    }
    else
    {
        strcpy(descp, list->desc);
        
        if(strlen(descp) < 24)
        {
            while(strlen(descp) < 24)
            {
                strcat(descp, " ");
            }
        }
    }
    fprintf(stdout,"| %s ",descp);
    free(descp);

}

/*******************AMOUNT PRINT************************************/

void printamount(double amount,int flag)
{
    char* amountStr;
    int chk=0;
    amountStr = (char*) malloc (25);
    
    if(amount > 10000000)
    {
        amountStr = " ?,???,???.?? ";
        chk=1;
    }
    else if (amount < -10000000)
    {
        strcat(amountStr,"(");
        strcat(amountStr,"?,???,???.??");
        strcat(amountStr,")");
    }
    else
    {
        strfmon(amountStr, 24, "%!(14#7.2n", amount);
    }
    if(flag==0)
    {
        fprintf(stdout,"| %s ",amountStr);
    }
    else
    {
        fprintf(stdout,"| %s |\n",amountStr);
    }
    if(chk==1)
    {
        if(flag==1)
        {
            fprintf(stderr,"ERROR: Cannot compute further due to overflow of balance\n");
            exit(1);
        }
    }
    free(amountStr);
}

void display(My402List* final_list)
{
    setlocale(LC_ALL, "en_CA.UTF-8");
    My402ListElem *element= NULL;
    Data *tempData = NULL;
    
    double balance = 0;
    
    fprintf(stdout,"+-----------------+--------------------------+----------------+----------------+\n");
    fprintf(stdout,"|       Date      | Description              |         Amount |        Balance |\n");
    fprintf(stdout,"+-----------------+--------------------------+----------------+----------------+\n");

    

    for(element=My402ListFirst(final_list); element!=NULL; element=My402ListNext(final_list,element))
    {

            tempData=(Data*)element->obj;
        
      
            printtime(tempData);

            
            printdescription(tempData);
            
            int amt=(tempData->tran_amt);
            double amount=(double)amt/100;
            
            if((tempData->type)=='-')      //=====DID CORRECTION======//
            {
                amount=-amount;
            }
        
            int flag=0;
            printamount(amount,flag);
            
            balance=balance+amount;
            flag=1;
            printamount(balance,flag);
            
    }
    fprintf(stdout,"+-----------------+--------------------------+----------------+----------------+\n");
    
}

 /********************ARGUMENT CHECK**********************/

FILE* argChk(int argc, char* argv[])
{
    FILE* fp = NULL;
    
    if(argc>3)  //if more than 2 arguments are passed..//
    {
        fprintf(stderr,"ERROR: 1.Malformed commands\n");
        fprintf(stderr, "Usage: warmup1 sort [filename]\n");
        exit(0);
    }
    
    else if(argc==2)    //....if file is not provided...//
    {
        if(strcmp(argv[1],"sort")!=0)  //****checking if the first argument is sort...//
        {
            fprintf(stderr,"ERROR: 2.Malformed commands\n");
            fprintf(stderr, "Usage: warmup1 sort [filename]\n");
            exit(0);
        }
        fp=stdin;
    }
    else if(argc==3)    //****if inputting from the file****//
    {
        if(strcmp(argv[1],"sort")!=0 || argv[1]==NULL)
        {
            fprintf(stderr,"ERROR: 3.Malformed commands\n");
            fprintf(stderr, "Usage: warmup1 sort [filename]\n");
            exit(0);
        }
        fp=fopen(argv[2],"r");  //****opening file in read mode****//
        
        struct stat file_err;
        stat(argv[2],&file_err);
        if( file_err.st_mode & S_IFDIR )
        {
            printf("\nInput file %s is a directory \n",argv[2]);
            exit(-1);
        }
        
        
        if(errno==ENOENT)
        {
            printf("\nInput file %s does not exist\n",argv[2]);
            exit(-1);
        }
        
        if(errno==EACCES)
        {
            printf("\nInput file %s cannot be opened - access denies\n",argv[2]);
            exit(-1);
        }
        

    }
    else
    {
        fprintf(stderr,"ERROR: 4.Malformed commands\n");
        fprintf(stderr, "Usage: warmup1 sort [filename]\n");
        exit(0);
    }
    
    return fp;
}

void cleanup(My402List* list)
{
    My402ListUnlinkAll(list);
    free(list);
}


int main(int argc,char* argv[])
{
    FILE* fp=NULL;
    fp=argChk(argc,argv);

    if(fp == NULL || fp == 0)
	{
		fprintf(stderr, "Quitting the Program because - %s\n", strerror(errno));
		exit(0);
	}
 
    
    My402List *list;        //------creating a list-----//
    list=(My402List*)malloc(sizeof(My402List));
    
    if(!My402ListInit(list))
    {
        fprintf(stderr,"Error: Error in Initializing the list\n");
        exit(0);
    }
        if(!Read_input(fp,list))
    {
        printf("error\n");
        exit(0);
    }

        if(list->num_members==0)
        {
            fprintf(stderr,"Error:Invalid input\n");
            exit(0);
        }
        sort(list);
        display(list);
        fclose(fp);
    cleanup(list);
       
        return TRUE;
    
        
        
        
}
    
    
    
    
    
    

