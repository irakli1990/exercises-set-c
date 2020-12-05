import java.util.concurrent.Semaphore;

class Pot {
    static final int M = 5; // Pot capacity
    private Semaphore emptyPot = new Semaphore(1);
    private Semaphore available = new Semaphore(0);
    private int servingsAvailable = 0;
    private int totalServedCount = 0;

    private synchronized void insertServings(int value) {
        servingsAvailable = value;
    }

    private synchronized int removeServing() {
        --servingsAvailable;
        ++totalServedCount;
        return servingsAvailable;
    }

    public int getTotalServedCount() {
        return totalServedCount;
    }

    public void getServing() throws InterruptedException {
        available.acquire();
        if (removeServing() == 0) {
            emptyPot.release();
        }
    }

    public void fill() throws InterruptedException {
        emptyPot.acquire();
        insertServings(M);
        available.release(M);
    }
}

class Person extends Thread {
    Pot pot;
    int servingsConsumed = 0;

    public Person(String name, Pot pot) {
        super(name);
        this.pot = pot;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < 100; ++i) {
                pot.getServing();
                ++servingsConsumed;
                Thread.yield();
            }
        } catch (InterruptedException e) {
            return;
        }
    }
}

class Cook extends Thread {
    Pot pot;

    public Cook(Pot pot) {
        this.pot = pot;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                pot.fill();
            }
        } catch (InterruptedException e) {
            return;
        }
    }
}

public class Feast {
    public static void main(String[] args) throws InterruptedException {
        Pot pot = new Pot();
        Cook cook = new Cook(pot);
        final int N = 10;
        Person[] people = new Person[N];
        for (int i = 0; i < people.length; ++i) {
            people[i] = new Person("Person " + i, pot);
        }
        cook.start();
        for (Thread t : people) {
            t.start();
        }
        for (Thread t : people) {
            t.join();
        }

        cook.interrupt();
        System.out.printf("Total served: %d.\n", pot.getTotalServedCount());
        for (Person p : people) {
            System.out.printf("[%s] Ate %d servings.\n", p.getName(), p.servingsConsumed);
        }
        System.out.println("Finishing simulation.");
    }
}