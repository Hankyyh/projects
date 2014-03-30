Group Members:
Saurabh Verma (saurabhv@usc.edu)
Vineet Gadodia (gadodia@usc.edu)
Siddarth Gupta (siddartg@usc.edu)
Natang Salgia (nsalgia@usc.edu)

Test Cases:
1. It has 7 sub cases, all in one file kmain.c in test1 folder. It Creates a child process of init and:
1a. The function that it calls returns NULL.
1b. The function that it calls, calls do_exit().
1c. The function that it calls cancels itself.
1d. The function that it calls, calls kthread_exit().
1e. The function that it calls, locks and unlocks mutex.
1f. The function that it calls locks mutex and exits.
1g. The function that it calls, calls do_waitpid(). Since it has no child process KASSERT fails and program halts.

2a. Init creates process3 which creates process4 which creates process5.
2b. Same as 2a but process3 waits for process4
2c. Same as 2a but process4 waits for process5
2d. Same as 2a but process3 waits for process4 and process4 waits for process5
2e. Same as 2d but process5 cancels process3

3. Same as 2a but process5 calls proc_kill_all()

4. Init creates process3 which creates process4 and locks global mutex m. After this does do_waitpid() for process4. Process4 when in context also tries to lock mutex m creating deadlock. Process5 is redundant

5. Init creates process3 that locks global mutex m, unlocks it and then waits for child process4. Process4 locks mutex m, waits for child process5 and then unlocks mutex m.

6. Init creates process3 that locks global mutex m, and calls for child process4 before releasing it. Process4 also requests lock for the same mutex but using cancellable lock. Child process5 of process4, cancels process4 saving the process from deadlock.

7. Init creates process3 that locks global mutex m and is out in sleep on OUR queue. process4 also requests for the same mutex and is put in mutex queue. process5 removes process3 from OUR queue and puts it into runnable, resuming the processes.

8. Same as 7 except for process3 blocks on OUR queue in cancellable state and process4 tries to lock mutex using cancellable lock. process5 calls kthread_cancel on process3.

9. Same as 8 except for process5 cancels thread of process4 that had called kmutex_lock_cancellable.

/*************************************TEST FILES PLACEMENT*******************************************/

We have made kmain.c for every test case. Each of these files have been placed in folders named test1, test2a, test2b, test2c, test2d, test2e, test3, test4, test5, test6, test7, test8, test9. Original kmain.c file is in "original kmain" as backup.

For test1:
All the 7 sub tests are in the same function: p3_run(). The grader needs to uncomment the sub test he wants to test and comment rest of it. By default test 1a is enabled. To run specific sub test uncomment the code between following delimiters: /*sub case # starts*/ /*sub case # ends*/

/*sub case # starts*/

  /* code for case # */

/*sub case # ends*/

Sorry for the inconvenience caused, but adding all tests in one file with switch case was making it too messy and difficult to interpret.

Files Submitted:

Config.mk
procs-README.txt
kmain.c
kmutex.c
proc.c
sched.c
kthread.c

tests.tar.gz:
test1 : kmain.c
test2a : kmain.c 
test2b : kmain.c 
test2c : kmain.c 
test2d : kmain.c 
test2e : kmain.c
test3 : kmain.c 
test4 : kmain.c 
test5 : kmain.c 
test6 : kmain.c
test7 : kmain.c 
test8 : kmain.c
test9 : kmain.c
original kmain : kmain.c