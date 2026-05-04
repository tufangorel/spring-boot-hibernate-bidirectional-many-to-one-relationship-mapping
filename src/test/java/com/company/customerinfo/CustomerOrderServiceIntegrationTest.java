package com.company.customerinfo;


import com.company.customerinfo.model.Customer;
import com.company.customerinfo.model.CustomerOrder;
import com.company.customerinfo.model.OrderItem;
import com.company.customerinfo.model.ShippingAddress;
import com.company.customerinfo.service.CustomerOrderService;
import com.company.customerinfo.service.CustomerService;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = CustomerInfoApplication.class)
@ActiveProfiles("dev")
public class CustomerOrderServiceIntegrationTest {

    @Autowired
    private CustomerService customerService;
    @Autowired
    private CustomerOrderService customerOrderService;

    @Order(1)
    @Test
    public void saveCustomerWithOrdersTest() {

        Customer customer = new Customer();
        customer.setName("name1");
        customer.setAge(25);

        ShippingAddress shippingAddress = new ShippingAddress();
        shippingAddress.setCountry("TR");
        shippingAddress.setCity("Ankara");
        shippingAddress.setStreetName("KaleSokak");
        customer.setShippingAddress(shippingAddress);

        Customer savedCustomerRecord = customerService.save(customer, "test-customer-key-3");
        assertThat( savedCustomerRecord.getShippingAddress() != null);

        CustomerOrder customerOrder = new CustomerOrder();
        customerOrder.setCustomer(customer);
        customerOrder.setOrderDate(LocalDateTime.now());
        customerOrder.setTitle("Order-001");

        OrderItem orderItem1 = new OrderItem();
        orderItem1.setQuantity(1);
        orderItem1.setCustomerOrder(customerOrder);
        OrderItem orderItem2 = new OrderItem();
        orderItem2.setQuantity(2);
        orderItem2.setCustomerOrder(customerOrder);

        customerOrder.addOrderItem(orderItem1);
        customerOrder.addOrderItem(orderItem2);

        CustomerOrder savedCustomerOrder = customerOrderService.save(customerOrder, "test-order-key-1");

        assertThat(savedCustomerOrder).isNotNull();
    }

    @Order(2)
    @Test
    public void saveCustomerOrderWithIdempotencyKeyTest() {

        Customer customer = new Customer();
        customer.setName("name2");
        customer.setAge(30);

        ShippingAddress shippingAddress = new ShippingAddress();
        shippingAddress.setCountry("TR");
        shippingAddress.setCity("Istanbul");
        shippingAddress.setStreetName("Barbaros");
        customer.setShippingAddress(shippingAddress);

        Customer savedCustomerRecord = customerService.save(customer, "test-customer-key-4");
        assertThat(savedCustomerRecord).isNotNull();

        CustomerOrder firstOrder = new CustomerOrder();
        firstOrder.setCustomer(savedCustomerRecord);
        firstOrder.setOrderDate(LocalDateTime.now());
        firstOrder.setTitle("Order-002");

        OrderItem orderItem1 = new OrderItem();
        orderItem1.setQuantity(1);
        orderItem1.setCustomerOrder(firstOrder);
        OrderItem orderItem2 = new OrderItem();
        orderItem2.setQuantity(2);
        orderItem2.setCustomerOrder(firstOrder);

        firstOrder.addOrderItem(orderItem1);
        firstOrder.addOrderItem(orderItem2);

        String idempotencyKey = "customer-order-key-123";
        CustomerOrder savedFirst = customerOrderService.save(firstOrder, idempotencyKey);

        CustomerOrder secondOrder = new CustomerOrder();
        secondOrder.setCustomer(savedCustomerRecord);
        secondOrder.setOrderDate(savedFirst.getOrderDate());
        secondOrder.setTitle("Order-002");

        OrderItem secondItem1 = new OrderItem();
        secondItem1.setQuantity(1);
        secondItem1.setCustomerOrder(secondOrder);
        OrderItem secondItem2 = new OrderItem();
        secondItem2.setQuantity(2);
        secondItem2.setCustomerOrder(secondOrder);

        secondOrder.addOrderItem(secondItem1);
        secondOrder.addOrderItem(secondItem2);

        CustomerOrder savedSecond = customerOrderService.save(secondOrder, idempotencyKey);

        assertThat(savedSecond).isNotNull();
        assertThat(savedSecond.getId()).isEqualTo(savedFirst.getId());
        assertThat(savedSecond.getTitle()).isEqualTo(savedFirst.getTitle());
    }
}
