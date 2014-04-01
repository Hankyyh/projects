#include<stdio.h>
#include<stdlib.h>
#include "my402list.h"
#include "cs402.h"
#include<string.h>

extern int My402ListLength(My402List* my402list)
{
	return my402list->num_members;//no of elements
}

extern int My402ListInit(My402List* my402list)
{
	My402ListElem* anch = &(my402list->anchor);		//start element of the list
	if(anch==0)
	{
		return 0;	//return false if not created
	}
	else
	{
		my402list->num_members=0;	//initialization
		anch->obj=NULL;
		anch->next=anch;		//anchor pointing to itself
		anch->prev=anch;
		return TRUE;
	}
}

extern int My402ListEmpty(My402List* my402list)
{
	
	if(my402list->num_members==0)
	{	
		return TRUE;
	}
	else
	{	return 0;}
	
}

extern int My402ListAppend(My402List* my402list, void* object)
{

	My402ListElem* lastElement = My402ListLast(my402list);
    
	My402ListElem* newElement = (My402ListElem*) malloc (sizeof(My402ListElem));
    
	if(newElement == 0)
	{
		return 0;
	}
    if(lastElement==NULL)
    {
        lastElement=&(my402list->anchor);
    }
	newElement->obj = object;
	
	lastElement->next = newElement;
	my402list->anchor.prev = newElement;
    
	newElement->next = &(my402list->anchor);
	newElement->prev = lastElement;
    
	my402list->num_members++;
	return TRUE;
}
    


extern int My402ListPrepend(My402List* my402list, void* object)
{
    
	My402ListElem* firstElement = My402ListFirst(my402list);
    
	My402ListElem* newElement = (My402ListElem*) malloc (sizeof(My402ListElem));
    
	if(newElement == 0)
	{
		return 0;
	}
    if(firstElement == NULL)//if its an anchor
	{
		firstElement = &(my402list->anchor);
	}
	newElement->obj = object;
    
	my402list->anchor.next = newElement;
	firstElement->prev = newElement;
    
	newElement->next = firstElement;
	newElement->prev = &(my402list->anchor);
    
	my402list->num_members++;
	return TRUE;}

extern void My402ListUnlink(My402List* my402list, My402ListElem* my402listElem)
{
	
	if(my402list->num_members==0)
	{
		return;
	}
	else
    {
        My402ListElem* prevElement=my402listElem->prev;
        My402ListElem* nextElement=my402listElem->next;
        prevElement->next=nextElement;
        nextElement->prev=prevElement;
        free(my402listElem);
        my402list->num_members--;
    }
}

extern void My402ListUnlinkAll(My402List* my402list)
{
    
	if(my402list->num_members==0)
	{
		return;
	}
	
    My402ListElem* firstElement=My402ListFirst(my402list);
	My402ListElem* ptr;	//declaring a pointer to traverse the list
	
	for(ptr=firstElement;ptr!=NULL;ptr=My402ListNext(my402list,ptr))
	{
		free(ptr);
        my402list->num_members--;
    }
    
}

extern int My402ListInsertAfter(My402List* my402list, void* object, My402ListElem* my402ListElement)
{
	
	 My402ListElem* newElement= (My402ListElem*) malloc(sizeof(My402ListElem));
	newElement->obj=object;
	if(my402ListElement==NULL)	//if no such element present then append
	{
		My402ListAppend(my402list,object);
		return TRUE;
	}
	else	//retreive the next element and insert it in between the two
	{
		My402ListElem* nextElement= my402ListElement->next;
        
		my402ListElement->next=newElement;
		newElement->prev=my402ListElement;
		newElement->next=nextElement;
        nextElement->prev=newElement;
		my402list->num_members++;
		return TRUE;
	}
	return 0;	//false if operation is unsuccessful
}

extern int My402ListInsertBefore(My402List* my402list, void* object, My402ListElem* my402ListElement)
{
	 My402ListElem* newElement= (My402ListElem*) malloc(sizeof(My402ListElem));
        newElement->obj=object;
	if(my402ListElement==NULL)
	{
			My402ListPrepend(my402list,object);
			return TRUE;
	}
	else
	{
		My402ListElem* prevElem=my402ListElement->prev;
        
		prevElem->next=newElement;
		newElement->prev=prevElem;
		newElement->next=my402ListElement;
        my402ListElement->prev=newElement;
		my402list->num_members++;
		return TRUE;
	}
		return 0;
}

extern My402ListElem *My402ListFirst(My402List* my402list)
{
	if(my402list->num_members==0)
	{
		return NULL;
	}	
	else
		return my402list->anchor.next;
}

extern My402ListElem *My402ListLast(My402List* my402list)
{
	if(my402list->num_members==0)
	{
		return NULL;
	}
	else
		return my402list->anchor.prev;
}
extern My402ListElem *My402ListNext(My402List* my402list,My402ListElem* my402listElement)
{
	if(my402listElement->next==&(my402list->anchor))	//if its the last element return null
	{
		return NULL;
	}
	else
		return my402listElement->next;
}

extern My402ListElem *My402ListPrev(My402List* my402list,My402ListElem* my402listElement)
{
        //if(my402listElement==My402ListFirst(my402list))	//if its the first element return null
     if(my402listElement->prev==&(my402list->anchor))        {
                return NULL;
        }
        else
                return my402listElement->prev;
}

extern My402ListElem *My402ListFind(My402List* my402list, void* object)
{
	if(my402list->num_members==0)	//if list is empty 
	{
		return NULL;
	}
	My402ListElem* ptr;
	ptr=NULL;
	for(ptr=My402ListFirst(my402list);ptr!=NULL;ptr=My402ListNext(my402list,ptr))	//traversing the list
	{
		if(ptr->obj==object)	//if both the locations are same
		{
			return ptr;
		}
	}
		return NULL;
} 


