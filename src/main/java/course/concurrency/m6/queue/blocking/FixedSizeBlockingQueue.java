package course.concurrency.m6.queue.blocking;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class FixedSizeBlockingQueue<T> implements BlockingQueue<T> {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition isNotEmpty = lock.newCondition();
    private final Condition isNotFull = lock.newCondition();

    private volatile Node<T> head;
    private volatile Node<T> tail;
    private final long capacity;
    private volatile long size = 0;

    public FixedSizeBlockingQueue(long capacity) {
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
            lock.lockInterruptibly();
            // wait till the queue is not full
            while (size == capacity) {
                isNotFull.await();
            }

            Node<T> newNode = new Node<>(value);
            if (tail != null) {
                tail.setPrev(newNode);
                newNode.setNext(tail);
                tail = newNode;
            } else {
                head = tail = newNode;
            }
            ++size;
            isNotEmpty.signal();
            lock.unlock();
        } catch (InterruptedException interruptedException) {
            log.info("{} element enqueuing was interrupted", value);
            Thread.currentThread().interrupt();
        }

    }

    @Override
    public T dequeue() {
        T value = null;
        try {
            lock.lockInterruptibly();
            // wait till the queue is not empty
            while (head == null) {
                isNotEmpty.await();
            }

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
            isNotFull.signal();
            lock.unlock();
        } catch (InterruptedException interruptedException) {
            log.info("Element dequeuing was interrupted");
            Thread.currentThread().interrupt();
        }
        return value;
    }

    long size() {
        return size;
    }
}
