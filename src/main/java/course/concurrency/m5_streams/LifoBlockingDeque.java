package course.concurrency.m5_streams;

import java.util.concurrent.LinkedBlockingDeque;

public class LifoBlockingDeque<T> extends LinkedBlockingDeque<T> {

    @Override
    public T remove() {
        return super.removeLast();
    }

    @Override
    public T poll() {
        return super.pollLast();
    }

    @Override
    public T take() throws InterruptedException {
        return super.takeLast();
    }
}
