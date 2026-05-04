package com.company.customerinfo.controller;

import com.company.customerinfo.CustomerInfoApplication;
import com.company.customerinfo.model.Customer;
import com.company.customerinfo.repository.CustomerOrderRepository;
import com.company.customerinfo.repository.CustomerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CustomerInfoApplication.class)
@ActiveProfiles("dev")
class CustomerOrderIdempotencyIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        customerOrderRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void saveCustomerOrderTwiceWithSameIdempotencyKeyReturnsSameResource() throws Exception {
        Customer customer = new Customer();
        customer.setName("integration-customer");
        customer.setAge(35);
        Customer savedCustomer = customerRepository.save(customer);

        String idempotencyKey = "integration-order-key-001";
        String payload = "{" +
                "\"title\":\"Order-Integration\"," +
                "\"orderDate\":\"" + LocalDateTime.now().withNano(0) + "\"," +
                "\"customer\":{\"id\":" + savedCustomer.getId() + "}," +
                "\"orderItems\":[{\"quantity\":1},{\"quantity\":2}]" +
                "}";

        String firstResponse = mockMvc.perform(post("/customerorder/save")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode firstBody = objectMapper.readTree(firstResponse);
        assertThat(firstBody.get("id")).isNotNull();
        int firstId = firstBody.get("id").asInt();

        String secondResponse = mockMvc.perform(post("/customerorder/save")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode secondBody = objectMapper.readTree(secondResponse);
        assertThat(secondBody.get("id")).isNotNull();
        int secondId = secondBody.get("id").asInt();

        assertThat(secondId).isEqualTo(firstId);
        assertThat(secondBody.get("title").asText()).isEqualTo(firstBody.get("title").asText());
    }
}
