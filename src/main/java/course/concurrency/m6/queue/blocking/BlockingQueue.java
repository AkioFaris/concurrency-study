package course.concurrency.m6.queue.blocking;

public interface BlockingQueue<T> {

    void enqueue(T value);
    T dequeue();
}
