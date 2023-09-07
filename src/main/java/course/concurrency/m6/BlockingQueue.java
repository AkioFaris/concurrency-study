package course.concurrency.m6;

public interface BlockingQueue<T> {

    void enqueue(T value);
    T dequeue();
}
