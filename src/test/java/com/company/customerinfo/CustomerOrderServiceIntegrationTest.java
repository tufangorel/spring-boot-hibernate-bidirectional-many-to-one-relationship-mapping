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
        customer.setAge(1);

        ShippingAddress shippingAddress = new ShippingAddress();
        shippingAddress.setCountry("TR");
        shippingAddress.setCity("Ankara");
        shippingAddress.setStreetName("KaleSokak");
        customer.setShippingAddress(shippingAddress);

        Customer savedCustomerRecord = customerService.save(customer);
        assertThat( savedCustomerRecord.getShippingAddress() != null);

        CustomerOrder customerOrder = new CustomerOrder();
        customerOrder.setCustomer(customer);
        customerOrder.setOrderDate(LocalDateTime.now());

        OrderItem orderItem1 = new OrderItem();
        orderItem1.setQuantity(1);
        orderItem1.setCustomerOrder(customerOrder);
        OrderItem orderItem2 = new OrderItem();
        orderItem2.setQuantity(2);
        orderItem2.setCustomerOrder(customerOrder);

        customerOrder.addOrderItem(orderItem1);
        customerOrder.addOrderItem(orderItem2);

        CustomerOrder savedCustomerOrder = customerOrderService.save(customerOrder);

        assertThat( savedCustomerOrder != null);
    }
}
