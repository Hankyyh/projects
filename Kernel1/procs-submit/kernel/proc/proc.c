#include "kernel.h"
#include "config.h"
#include "globals.h"
#include "errno.h"

#include "util/debug.h"
#include "util/list.h"
#include "util/string.h"
#include "util/printf.h"

#include "proc/kthread.h"
#include "proc/proc.h"
#include "proc/sched.h"
#include "proc/proc.h"

#include "mm/slab.h"
#include "mm/page.h"
#include "mm/mmobj.h"
#include "mm/mm.h"
#include "mm/mman.h"

#include "vm/vmmap.h"

#include "fs/vfs.h"
#include "fs/vfs_syscall.h"
#include "fs/vnode.h"
#include "fs/file.h"

#include "mm/kmalloc.h"

proc_t *curproc = NULL; /* global */
static slab_allocator_t *proc_allocator = NULL;

static list_t _proc_list;
static proc_t *proc_initproc = NULL; /* Pointer to the init process (PID 1) */

void
proc_init()
{
        list_init(&_proc_list);
        proc_allocator = slab_allocator_create("proc", sizeof(proc_t));
        KASSERT(proc_allocator != NULL);
}

static pid_t next_pid = 0;

/**
 * Returns the next available PID.
 *
 * Note: Where n is the number of running processes, this algorithm is
 * worst case O(n^2). As long as PIDs never wrap around it is O(n).
 *
 * @return the next available PID
 */
static int
_proc_getid()
{
        proc_t *p;
        pid_t pid = next_pid;
        while (1) {
failed:
                list_iterate_begin(&(_proc_list), p, proc_t, p_list_link) {
                        if (p->p_pid == pid) {
                                if ((pid = (pid + 1) % PROC_MAX_COUNT) == next_pid) {
                                        return -1;
                                } else {
                                        goto failed;
                                }
                        }
                } list_iterate_end();
                next_pid = (pid + 1) % PROC_MAX_COUNT;
                return pid;
        }
}

/*
 * The new process, although it isn't really running since it has no
 * threads, should be in the PROC_RUNNING state.
 *
 * Don't forget to set proc_initproc when you create the init
 * process. You will need to be able to reference the init process
 * when reparenting processes to the init process.
 */
proc_t *
proc_create(char *name)
{

	proc_t *process = (proc_t *)slab_obj_alloc(proc_allocator);
	KASSERT(NULL != process);
	int id = _proc_getid();
    
	if(id!=-1){
        KASSERT(PID_IDLE != id || list_empty(&_proc_list));
        KASSERT(PID_INIT != id || PID_IDLE == curproc->p_pid);
		process->p_pid = id;

        strncpy(process->p_comm, name,sizeof(process->p_comm)-1);
        
        list_init(&(process->p_threads));
        
        list_init(&(process->p_children));
        if (id == 0)
        {
            process->p_pproc = NULL;
        }else{
            process->p_pproc = curproc;
        }
        process->p_state = PROC_RUNNING;
        process->p_status = 0;
        sched_queue_init(&(process->p_wait));
        process->p_pagedir = pt_create_pagedir();

        list_link_init(&(process->p_list_link));
        list_insert_tail(&(_proc_list), &(process->p_list_link));
        if(process->p_pproc != NULL){
            list_link_init(&(process->p_child_link));
            list_insert_tail(&((process->p_pproc)->p_children), &(process->p_child_link));
        }
        
        process->p_files[NFILES] = NULL;

        process->p_cwd=NULL;           /* current working dir */

        /* VM */
        process->p_brk = NULL;           /* process break; see brk(2) */
        process->p_start_brk = NULL;     /* initial value of process break */
        process->p_vmmap = NULL;

        if (id == 1)
        {
            proc_initproc = process;
        }

		return process;
	}
        return NULL;
}

/**
 * Cleans up as much as the process as can be done from within the
 * process. This involves:
 *    - Closing all open files (VFS)
 *    - Cleaning up VM mappings (VM)
 *    - Waking up its parent if it is waiting
 *    - Reparenting any children to the init process
 *    - Setting its status and state appropriately
 *
 * The parent will finish destroying the process within do_waitpid (make
 * sure you understand why it cannot be done here). Until the parent
 * finishes destroying it, the process is informally called a 'zombie'
 * process.
 *
 * This is also where any children of the current process should be
 * reparented to the init process (unless, of course, the current
 * process is the init process. However, the init process should not
 * have any children at the time it exits).
 *
 * Note: You do _NOT_ have to special case the idle process. It should
 * never exit this way.
 *
 * @param status the status to exit the process with
 */
void
proc_cleanup(int status)
{
    KASSERT(NULL != proc_initproc);
    KASSERT(1 <= curproc->p_pid);
    KASSERT(NULL != curproc->p_pproc);

    curproc->p_state = PROC_DEAD;
    curproc->p_status = status;

    proc_t *pproc = curproc->p_pproc;
    kthread_t *pthread = list_head(&pproc->p_threads,kthread_t,kt_plink);

    if (!sched_queue_empty(&(curproc->p_wait)))
    {
        dbg_print("parent %d was waiting for child process %d\n",pproc->p_pid,curproc->p_pid);
        sched_wakeup_on(&(curproc->p_wait));
        /*!sched_queue_empty(&(pproc->p_wait))*/
    }else if(&pproc->p_wait == pthread->kt_wchan){
        dbg_print("parent %d was waiting for pid -1\n",pproc->p_pid);
        sched_wakeup_on(&(pproc->p_wait));
    }

    /* reparenting children to init*/
    if (curproc != proc_initproc)
    {
        proc_t * child = NULL;
        list_iterate_begin(&curproc->p_children, child, proc_t, p_child_link)
        {
            list_remove(&(child->p_child_link));
            list_link_init(&(child->p_child_link));
            list_insert_tail(&(proc_initproc->p_children), &(child->p_child_link));
            child->p_pproc = proc_initproc;
        }list_iterate_end();
    }
    sched_switch();
}

/*
 * This has nothing to do with signals and kill(1).
 *
 * Calling this on the current process is equivalent to calling
 * do_exit().
 *
 * In Weenix, this is only called from proc_kill_all.
 */
void
proc_kill(proc_t *p, int status)
{
    kthread_t *k;
    list_iterate_begin(&(p->p_threads), k, kthread_t, kt_plink) {
        if(curthr!=k)
            {
                kthread_cancel(k, k->kt_retval);
            }
        } list_iterate_end();
}

/*
 * Remember, proc_kill on the current process will _NOT_ return.
 * Don't kill direct children of the idle process.
 *
 * In Weenix, this is only called by sys_halt.
 */
void
proc_kill_all()
{
    proc_t *p;
    list_iterate_begin(&_proc_list, p, proc_t, p_list_link) {
            if(p->p_pid != 0 && p->p_pproc!=NULL && p->p_pproc->p_pid != PID_IDLE)
            {
                proc_kill(p, p->p_status);
            }
    } list_iterate_end();
    kthread_cancel(curthr,0);
}

proc_t *
proc_lookup(int pid)
{
        proc_t *p;
        list_iterate_begin(&_proc_list, p, proc_t, p_list_link) {
                if (p->p_pid == pid) {
                        return p;
                }
        } list_iterate_end();
        return NULL;
}

list_t *
proc_list()
{
        return &_proc_list;
}

/*
 * This function is only called from kthread_exit.
 *
 * Unless you are implementing MTP, this just means that the process
 * needs to be cleaned up and a new thread needs to be scheduled to
 * run. If you are implementing MTP, a single thread exiting does not
 * necessarily mean that the process should be exited.
 */
void
proc_thread_exited(void *retval)
{
    proc_cleanup(curthr->kt_cancelled);
}

/* If pid is -1 dispose of one of the exited children of the current
 * process and return its exit status in the status argument, or if
 * all children of this process are still running, then this function
 * blocks on its own p_wait queue until one exits.
 *
 * If pid is greater than 0 and the given pid is a child of the
 * current process then wait for the given pid to exit and dispose
 * of it.
 *
 * If the current process has no children, or the given pid is not
 * a child of the current process return -ECHILD.
 *
 * Pids other than -1 and positive numbers are not supported.
 * Options other than 0 are not supported.
 */
pid_t
do_waitpid(pid_t pid, int options, int *status)
{
    dbg_print("do_waitpid by process %d for child process %d\n",curproc->p_pid,pid);
    
    list_link_t *link = &curproc->p_children;
    KASSERT(NULL!=link);
    proc_t *child = NULL;

        if(pid == -1){
            if (!list_link_is_linked(&(curproc->p_children)) || list_empty(&(curproc->p_children)))
            {
                return -1 * ECHILD;
            }else{
                int dead_child_flag = 0;
                for (link = curproc->p_children.l_next; link != &(curproc->p_children); link = link->l_next)
                {
                    child = list_item(link, proc_t, p_child_link);
                    KASSERT(NULL != child);
                    KASSERT(-1 == pid || child->p_pid == pid);
                    if (child->p_state == PROC_DEAD)
                    {
                        proc_t *child_in_parent;
                        list_iterate_begin(&_proc_list,child_in_parent,proc_t,p_list_link){
                            if (child_in_parent == child)
                            {
                                list_remove(&child_in_parent->p_list_link);
                            }
                        }list_iterate_end();
                        list_remove(link);
                        dead_child_flag = 1;
                        break;
                    }
                }
                if (dead_child_flag == 0)
                {
                    /*add parent to its own pwait and make the thread sleep on it*/
                    sched_sleep_on(&(curproc->p_wait));
                    for (link = curproc->p_children.l_next; link != &(curproc->p_children); link = link->l_next)
                    {
                        child = list_item(link, proc_t, p_child_link);
                        KASSERT(NULL != child);
                        KASSERT(-1 == pid || child->p_pid == pid);
                        if (child->p_state == PROC_DEAD)
                        {
                            proc_t *child_in_parent;
                            list_iterate_begin(&_proc_list,child_in_parent,proc_t,p_list_link){
                                if (child_in_parent == child)
                                    {
                                        list_remove(&child_in_parent->p_list_link);
                                    }
                            }list_iterate_end();
                            list_remove(link);
                            break;
                        }
                    }
                     kthread_t *t = list_item((child->p_threads.l_next),kthread_t,kt_plink);
                     KASSERT(KT_EXITED == t->kt_state);
                    kthread_destroy(t);
                    KASSERT(NULL != child->p_pagedir);
                    *status = child->p_status;
                    return child->p_pid;
                }else{
                    kthread_t *t = list_item((child->p_threads.l_next),kthread_t,kt_plink);
                    KASSERT(KT_EXITED == t->kt_state);
                    kthread_destroy(t);
                    KASSERT(NULL != child->p_pagedir);
                    *status = child->p_status;
                    return child->p_pid;
                }
            }
        }else if(pid > 0){
            if (!list_link_is_linked(&(curproc->p_children)) || list_empty(&(curproc->p_children)))
            {
                return -1 * ECHILD;
            }else{
                int child_flag = 0;
                
                for (link = curproc->p_children.l_next; link != &(curproc->p_children); link = link->l_next)
                {
                    child = list_item(link, proc_t, p_child_link);
                    KASSERT(NULL != child);
                    KASSERT(-1 == pid || child->p_pid == pid);
                    if (child->p_pid == pid)
                    {
                        child_flag = 1;

                        if (child->p_state == PROC_DEAD)
                        {
                            proc_t *child_in_parent;
                            list_iterate_begin(&_proc_list,child_in_parent,proc_t,p_list_link){
                            if (child_in_parent == child)
                            {
                                list_remove(&child_in_parent->p_list_link);
                            }
                        }list_iterate_end();
                            list_remove(link);
                            break;
                        }else{
                            /* add parent to the child's pwait and make the parent thread on this queue*/
                            sched_sleep_on(&(child->p_wait));

                            for (link = curproc->p_children.l_next; link != &(curproc->p_children); link = link->l_next)
                            {
                                child = list_item(link, proc_t, p_child_link);
                                KASSERT(NULL != child);
                                KASSERT(-1 == pid || child->p_pid == pid);
                                if (child->p_pid == pid)
                                 {
                                    if (child->p_state == PROC_DEAD)
                                    {
                                        proc_t *child_in_parent;
                                        list_iterate_begin(&_proc_list,child_in_parent,proc_t,p_list_link){
                                            if (child_in_parent == child)
                                            {
                                                list_remove(&child_in_parent->p_list_link);
                                            }
                                        }list_iterate_end();
                                        list_remove(link);
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
                if (child_flag == 0)
                {
                    return -1 * ECHILD;
                }else{
                    kthread_t *t = list_item((child->p_threads.l_next),kthread_t,kt_plink);
                    KASSERT(KT_EXITED == t->kt_state);
                    kthread_destroy(t);
                    KASSERT(NULL != child->p_pagedir);
                    *status = child->p_status;
                    return child->p_pid;
                }
            }
        }else{
            return -1;
        }
}

/*
 * Cancel all threads, join with them, and exit from the current
 * thread.
 *
 * @param status the exit status of the process
 */
void
do_exit(int status)
{
        curproc->p_status = status;
        kthread_cancel(curthr, &status);
}

size_t
proc_info(const void *arg, char *buf, size_t osize)
{
        const proc_t *p = (proc_t *) arg;
        size_t size = osize;
        proc_t *child;

        KASSERT(NULL != p);
        KASSERT(NULL != buf);

        iprintf(&buf, &size, "pid:          %i\n", p->p_pid);
        iprintf(&buf, &size, "name:         %s\n", p->p_comm);
        if (NULL != p->p_pproc) {
                iprintf(&buf, &size, "parent:       %i (%s)\n",
                        p->p_pproc->p_pid, p->p_pproc->p_comm);
        } else {
                iprintf(&buf, &size, "parent:       -\n");
        }

#ifdef __MTP__
        int count = 0;
        kthread_t *kthr;
        list_iterate_begin(&p->p_threads, kthr, kthread_t, kt_plink) {
                ++count;
        } list_iterate_end();
        iprintf(&buf, &size, "thread count: %i\n", count);
#endif

        if (list_empty(&p->p_children)) {
                iprintf(&buf, &size, "children:     -\n");
        } else {
                iprintf(&buf, &size, "children:\n");
        }
        list_iterate_begin(&p->p_children, child, proc_t, p_child_link) {
                iprintf(&buf, &size, "     %i (%s)\n", child->p_pid, child->p_comm);
        } list_iterate_end();

        iprintf(&buf, &size, "status:       %i\n", p->p_status);
        iprintf(&buf, &size, "state:        %i\n", p->p_state);

#ifdef __VFS__
#ifdef __GETCWD__
        if (NULL != p->p_cwd) {
                char cwd[256];
                lookup_dirpath(p->p_cwd, cwd, sizeof(cwd));
                iprintf(&buf, &size, "cwd:          %-s\n", cwd);
        } else {
                iprintf(&buf, &size, "cwd:          -\n");
        }
#endif /* __GETCWD__ */
#endif

#ifdef __VM__
        iprintf(&buf, &size, "start brk:    0x%p\n", p->p_start_brk);
        iprintf(&buf, &size, "brk:          0x%p\n", p->p_brk);
#endif

        return size;
}

size_t
proc_list_info(const void *arg, char *buf, size_t osize)
{
        size_t size = osize;
        proc_t *p;

        KASSERT(NULL == arg);
        KASSERT(NULL != buf);

#if defined(__VFS__) && defined(__GETCWD__)
        iprintf(&buf, &size, "%5s %-13s %-18s %-s\n", "PID", "NAME", "PARENT", "CWD");
#else
        iprintf(&buf, &size, "%5s %-13s %-s\n", "PID", "NAME", "PARENT");
#endif

        list_iterate_begin(&_proc_list, p, proc_t, p_list_link) {
                char parent[64];
                if (NULL != p->p_pproc) {
                        snprintf(parent, sizeof(parent),
                                 "%3i (%s)", p->p_pproc->p_pid, p->p_pproc->p_comm);
                } else {
                        snprintf(parent, sizeof(parent), "  -");
                }

#if defined(__VFS__) && defined(__GETCWD__)
                if (NULL != p->p_cwd) {
                        char cwd[256];
                        lookup_dirpath(p->p_cwd, cwd, sizeof(cwd));
                        iprintf(&buf, &size, " %3i  %-13s %-18s %-s\n",
                                p->p_pid, p->p_comm, parent, cwd);
                } else {
                        iprintf(&buf, &size, " %3i  %-13s %-18s -\n",
                                p->p_pid, p->p_comm, parent);
                }
#else
                iprintf(&buf, &size, " %3i  %-13s %-s\n",
                        p->p_pid, p->p_comm, parent);
#endif
        } list_iterate_end();
        return size;
}
