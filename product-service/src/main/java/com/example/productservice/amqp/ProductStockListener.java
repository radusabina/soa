package com.example.productservice.amqp;

import com.example.productservice.DTO.OrderCreatedEvent;
import com.example.productservice.DTO.OrderItemDto;
import com.example.productservice.Entity.Product;
import com.example.productservice.Repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ProductStockListener {

    private static final Logger log = LoggerFactory.getLogger(ProductStockListener.class);

    private final ProductRepository productRepository;

    public ProductStockListener(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        for (OrderItemDto item : event.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElse(null);

            if (product == null) {
                log.warn("Product with id {} not found, skipping", item.getProductId());
                continue; // nu blocăm mesajul, doar îl ignorăm
            }

            if (product.getStock() < item.getQuantity()) {
                log.warn("Not enough stock for product {}. Requested: {}, Available: {}",
                        product.getId(), item.getQuantity(), product.getStock());
                continue; // nu blocăm coada
            }

            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
            log.info("Decreased stock for product {}. New stock: {}", product.getId(), product.getStock());
        }
    }
}
