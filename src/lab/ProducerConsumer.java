package lab;

import java.util.concurrent.Semaphore;

/**
 Buffer is an implementation of a circular bounded buffer.
 At most 'capacity' items can be added using 'put()' each subsequent
 call will cause a thread to wait until some items are removed from
 the buffer through 'get()' calls.
 */
class Buffer {
    int [] items = null;
    Semaphore empty_slots = null;
    Semaphore taken_slots = null;
    int slot_index = 0;  // index of the next empty slot to put data
    int item_index = 0;  // index of the next taken slot

    public Buffer(int capacity) {
        items = new int[capacity];
        empty_slots = new Semaphore(capacity);  // All buffer slots are empty
        taken_slots = new Semaphore(0);         // None of them are occupied
    }

    public void put(int item) throws InterruptedException {
        empty_slots.acquire();  // If the buffer is full this will force the calling thread to wait

        // Now insert item safely into 'items' array.
        // We are using synchronized(items) to assure mutually exclusive access
        // to items and slot_index
        synchronized(items) {
            items[slot_index] = item;
            slot_index = (slot_index + 1) % items.length;  // Increment and wrap slot_index
        }

        // Inform consumer threads which called get() and are waiting
        // for new data to be produced
        taken_slots.release();
    }

    public int get() throws InterruptedException {
        taken_slots.acquire();

        // Pull an item safely from 'items' array
        int item;
        synchronized(items) {
            item = items[item_index];
            item_index = (item_index + 1) % items.length;
        }

        // Inform producer threads which called put() and are waiting for an empty slot
        empty_slots.release();
        return item;
    }

    boolean isEmpty() {
        return taken_slots.availablePermits() == 0;
    }
}

class Producer extends Thread {
    private int id;
    private Buffer buffer = null;
    private int production_size = 0;

    public Producer(int id_, Buffer buffer_, int production_size_) {
        id = id_;
        buffer = buffer_;
        production_size = production_size_;
    }

    // Producer inserts production_size copies of its id
    // into the shared buffer
    private void produce() throws InterruptedException {
        for (int i = 0; i < production_size; ++i) {
            buffer.put(id);
        }
    }

    @Override
    public  void run() {
        try {
            produce();
        } catch (InterruptedException e) {
        }
    }
}

class Consumer extends Thread {
    int id;
    Buffer buffer = null;
    int total_consumed = 0;

    public Consumer(int id_, Buffer buffer_) {
        id = id_;
        buffer = buffer_;
    }

    private void consume() throws InterruptedException {
        while (true) {
            total_consumed += buffer.get();
        }
    }

    @Override
    public void run() {
        try {
            consume();
        } catch (InterruptedException e) {
        }
    }
}


public class ProducerConsumer {
    public static void main(String[] args) throws InterruptedException {
        Buffer buffer = new Buffer(16);

        int producers_count = 3;
        int production_size = 100_000;

        Producer[] producers = new Producer[producers_count];
        for (int i = 0; i < producers_count; ++i) {
            producers[i] = new Producer(i+1, buffer, production_size);
        }

        int consumers_count = 10;
        Consumer[] consumers = new Consumer[consumers_count];
        for (int i = 0; i < consumers_count; ++i) {
            consumers[i] = new Consumer(i+1, buffer);
        }

        long start_time = System.nanoTime();

        // Start all consumer & producer threads
        for (Thread t : producers) { t.start(); }
        for (Thread t : consumers) { t.start(); }

        System.out.println("Waiting for producers to finish");
        for (Thread t : producers) { t.join(); }

        System.out.println("Waiting for consumers to empty the buffer");
        while (!buffer.isEmpty()) {
            Thread.sleep(0);
        }

        System.out.println("Interrupting the consumers");
        for (Thread t : consumers) {
            t.interrupt();  // This will stop a consumer that called 'buffer.get()'
        }

        long elapsed = System.nanoTime() - start_time;

        // Gather and check the results
        int total = 0;
        for (Consumer c : consumers) {
            System.out.printf("Consumer [%d] consumed: %d\n", c.id, c.total_consumed);
            total += c.total_consumed;
        }
        System.out.println("Total consumed by all: " + total);
        System.out.println("Total production: " + ((1 + producers_count)*producers_count/2) * production_size);
        System.out.println("Time elapsed: " + (elapsed / 1.0e9) + " sec.");
    }
}