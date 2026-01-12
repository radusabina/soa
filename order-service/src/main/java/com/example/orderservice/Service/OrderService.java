package com.example.orderservice.Service;

import com.example.orderservice.DTO.OrderCreatedEvent;
import com.example.orderservice.Entity.Order;
import com.example.orderservice.Entity.OrderItem;
import com.example.orderservice.Repository.OrderRepository;
import com.example.orderservice.amqp.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import com.example.orderservice.DTO.CreateOrderRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher publisher;

    public Order addOrder(CreateOrderRequest request) {

        Order order = new Order();
        order.setUserId(request.getUserId());

        List<OrderItem> items = request.getItems().stream()
                .map(i -> {
                    OrderItem item = OrderItem.builder()
                            .productId(i.getProductId())
                            .quantity(i.getQuantity())
                            .build();
                    item.setOrder(order);
                    return item;
                })
                .toList();

        order.setItems(items);

        double totalPrice = request.getItems().stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();

        order.setTotalPrice(totalPrice);

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setItems(request.getItems());

        publisher.publish(event);

        return orderRepository.save(order);
    }


    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

}

