package course.concurrency.m6;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.locks.ReentrantLock;

public class FixedSizeBlockingQueue<T> implements BlockingQueue<T> {

    private final ReentrantLock lock = new ReentrantLock();

    private volatile Node<T> head;
    private volatile Node<T> tail;
    private final long capacity;
    private volatile long size = 0;

    FixedSizeBlockingQueue(long capacity) {
        this.capacity = capacity;
    }

    @Setter
    @Getter
    static class Node<T> {
        T value;
        private Node<T> next;
        private Node<T> prev;

        Node(T value) {
            this.value = value;
        }
    }

    @Override
    public void enqueue(T value) {
        try {
            lock.lock();
            // do nothing if the queue is full
            if (size == capacity)
                return;

            Node<T> newNode = new Node<>(value);
            if (tail != null) {
                tail.setPrev(newNode);
                newNode.setNext(tail);
                tail = newNode;
            } else {
                head = tail = newNode;
            }
            ++size;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T dequeue() {
        T value;
        lock.lock();
        if (head != null) {
            value = head.getValue();
            // if it was the only element in the queue
            if (tail == head) {
                tail = null;
            }
            // move the head pointer to the preceding element in the queue
            head = head.getPrev();
            if (head != null) {
                head.setNext(null);
            }
            --size;
        } else {
            value = null;
        }
        lock.unlock();
        return value;
    }
}
