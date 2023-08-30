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

    private Order(Order otherOrder, PaymentInfo paymentInfo) {
        this.id = otherOrder.getId();
        this.items = List.copyOf(otherOrder.getItems());
        this.paymentInfo = paymentInfo;
        this.isPacked = otherOrder.isPacked();
        this.status = IN_PROGRESS;
    }

    private Order(Order otherOrder, boolean packed) {
        this.id = otherOrder.getId();
        this.items = List.copyOf(otherOrder.getItems());
        this.paymentInfo = otherOrder.getPaymentInfo();
        this.isPacked = packed;
        this.status = IN_PROGRESS;
    }

    private Order(Order otherOrder, Status status) {
        this.id = otherOrder.getId();
        this.items = List.copyOf(otherOrder.getItems());
        this.paymentInfo = otherOrder.getPaymentInfo();
        this.isPacked = otherOrder.isPacked();
        this.status = status;
    }

    public static Order packed(Order order) {
        return new Order(order, true);
    }

    public static Order payed(Order order, PaymentInfo paymentInfo) {
        return new Order(order, paymentInfo);
    }

    public static Order delivered(Order order) {
        return new Order(order, DELIVERED);
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
