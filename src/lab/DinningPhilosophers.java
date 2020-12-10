package lab;

import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;

//class DinningTable {
//    Lock [] fork_locks = null;
//    AtomicInteger fed_philosophers_count = new AtomicInteger(0);
//
//    public DinningTable(int forks_count) {
//        fork_locks = new Lock[forks_count];
//        for (int i = 0; i < forks_count; ++i) {
//            fork_locks[i] = new ReentrantLock();
//        }
//    }
//
//    private Lock getLeftForkLock(int philosopher_id) {
//        return fork_locks[philosopher_id];
//    }
//
//    private Lock getRightForkLock(int philosopher_id) {
//        return fork_locks[(philosopher_id + 1) % fork_locks.length];
//    }
//
//    public void pickUpForks(int philosopher_id) {
//        getLeftForkLock(philosopher_id).lock();
//        getRightForkLock(philosopher_id).lock();
//    }
//
//    public void putDownForks(int philosopher_id) {
//        getLeftForkLock(philosopher_id).unlock();
//        getRightForkLock(philosopher_id).unlock();
//    }
//}

class DinningTable {
    Lock[] forks = null;
    AtomicInteger fed_philosophers_count = new AtomicInteger(0);
    Semaphore dinning_hall = null;

    public DinningTable(int forks_count) {
        forks = new Lock[forks_count];
        for (int i = 0; i < forks_count; ++i) {
            forks[i] = new ReentrantLock();
        }
        dinning_hall = new Semaphore(forks_count - 1);  // Room for n-1 philosophers
    }

    private Lock getLeftForkLock(int philosopher_id) {
        return forks[philosopher_id];
    }

    private Lock getRightForkLock(int philosopher_id) {
        return forks[(philosopher_id + 1) % forks.length];
    }

    public void pickUpForks(int philosopher_id) {
        try {
            // First, try enter the "dinning room"
            dinning_hall.acquire();

            // Now we can safely try to pick the forks
            getLeftForkLock(philosopher_id).lock();
            getRightForkLock(philosopher_id).lock();
        } catch (InterruptedException e) {
        }
    }

    public void putDownForks(int philosopher_id) {
        getLeftForkLock(philosopher_id).unlock();
        getRightForkLock(philosopher_id).unlock();

        // We are leaving the dinning room
        dinning_hall.release();
    }

}
class Philosopher extends Thread {
    int id;
    DinningTable table  = null;
    int think_eat_count = 0;

    public Philosopher(int id_, DinningTable table_, int think_eat_count_) {
        super("Philosopher " + id_);
        id = id_;
        table = table_;
        think_eat_count = think_eat_count_;
    }

    public void eat() throws InterruptedException {
        System.out.printf("[%d] Eating\n", id);
        table.fed_philosophers_count.incrementAndGet();
        Thread.sleep(1);
    }

    public void think() {
        Thread.yield();  // Give the JVM a hint that it can switch to another thread
    }

    @Override
    public void run() {
        for (int i = 0; i < think_eat_count; ++i) {
            think();
            // Time to eat...
            try {
                table.pickUpForks(id);
                eat();
            } catch(InterruptedException e) {
                return ;  // We need to stop
            } finally {
                table.putDownForks(id);
            }
        }
    }
}

public class DinningPhilosophers {
    public static void main(String [] args) throws InterruptedException {
        int philosophers_count = 5;
        int think_eat_count = 50;

        DinningTable table = new DinningTable(philosophers_count);

        Thread [] philosophers = new Philosopher[philosophers_count];
        for (int i = 0; i < philosophers_count; ++i) {
            philosophers[i] = new Philosopher(i, table, think_eat_count);
            philosophers[i].start();
        }

        // Monitor the progress by checking if the number of times the philosophers
        // were fed successfuly changes
        int target_fed_count = philosophers_count * think_eat_count;

        boolean deadlock = false;
        int prev_count = table.fed_philosophers_count.get();
        while(true) {
            Thread.sleep(1000);  // Wait a second...

            int current_count = table.fed_philosophers_count.get();

            if (current_count == target_fed_count) {  // OK, all fed
                break ;
            }
            if (current_count == prev_count) {  // No progress was made since the prev. check
                System.out.println("Deadlock detected!");
                deadlock = true;
                break ;
            }
            prev_count = current_count;
        }

        if (deadlock) {
            // We are forcing the program to terminate
            System.err.println("Terminating program execution!");
            System.exit(-1);
        } else {  // Everything was OK, wait for the threads to finish their jobs
            System.out.println("All philosophers were fed successfuly :)");
            for (Thread t : philosophers) {
                t.join();
            }
        }
    }
}
