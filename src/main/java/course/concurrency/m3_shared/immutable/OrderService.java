package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

public class OrderService {

    private final ConcurrentHashMap<Long, Order> currentOrders = new ConcurrentHashMap<>();
    private final LongAdder nextId = new LongAdder();

    private long nextId() {
        nextId.increment();
        return nextId.sum();
    }

    public long createOrder(List<Item> items) {
        long id = nextId();
        currentOrders.put(id, new Order(id, items));
        return id;
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        Order updatedOrder = updateOrder(orderId, order -> new Order(order, paymentInfo));

        if (updatedOrder.isReadyForDelivery()) {
            deliver(updatedOrder);
        }
    }

    public void setPacked(long orderId) {
        Order updatedOrder = updateOrder(orderId, order -> new Order(order, true));

        if (updatedOrder.isReadyForDelivery()) {
            deliver(updatedOrder);
        }
    }

    private Order updateOrder(long orderId, Function<Order, Order> newOrderFunc) {
        Order updatedOrder;
        Order order;
        do {
            order = currentOrders.get(orderId);
            updatedOrder = newOrderFunc.apply(order);
        } while (!currentOrders.replace(orderId, order, updatedOrder));
        return updatedOrder;
    }

    private void deliver(Order order) {
        /* ... */
        currentOrders.put(order.getId(), new Order(order, Order.Status.DELIVERED));
    }

    public boolean isDelivered(long orderId) {
        return currentOrders.get(orderId).getStatus().equals(Order.Status.DELIVERED);
    }
}
