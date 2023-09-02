package course.concurrency.m3_shared.immutable;

import java.util.List;

import static course.concurrency.m3_shared.immutable.Order.Status.*;

public class Order {

    public enum Status { NEW, IN_PROGRESS, DELIVERED }

    private final Long id;
    private final List<Item> items;
    private final PaymentInfo paymentInfo;
    private final boolean isPacked;
    private final Status status;

    public Order(Long id, List<Item> items) {
        this.id = id;
        this.items = List.copyOf(items);
        this.paymentInfo = null;
        this.isPacked = false;
        this.status = NEW;
    }

    public Order(Long id, List<Item> items, PaymentInfo paymentInfo, boolean isPacked, Status status) {
        this.id = id;
        this.items = List.copyOf(items);
        this.paymentInfo = paymentInfo;
        this.isPacked = isPacked;
        this.status = status;
    }

    public static Order packed(Order order) {
        return new Order(order.getId(), order.getItems(), order.getPaymentInfo(),
                true, IN_PROGRESS);
    }

    public static Order payed(Order order, PaymentInfo paymentInfo) {
        return new Order(order.getId(), order.getItems(), paymentInfo,
                order.isPacked, IN_PROGRESS);
    }

    public static Order delivered(Order order) {
        return new Order(order.getId(), order.getItems(), order.getPaymentInfo(),
                order.isPacked(), DELIVERED);
    }

    public boolean isReadyForDelivery() {
        return items != null && !items.isEmpty() && paymentInfo != null && isPacked
                && !status.equals(DELIVERED);
    }

    public Long getId() {
        return id;
    }

    public List<Item> getItems() {
        return items;
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public boolean isPacked() {
        return isPacked;
    }

    public Status getStatus() {
        return status;
    }

}
