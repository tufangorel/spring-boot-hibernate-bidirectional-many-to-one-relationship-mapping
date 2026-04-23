package com.company.customerinfo.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.persistence.*;
import java.io.Serializable;

@Schema(description = "The model for customer")
@Entity
@Table(name = "customer")
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id", scope = Customer.class)
public class Customer implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Schema(description = "Name of the customer", required = true, example = "test name")
    @Column(name = "name", nullable = false)
    private String name;

    @Schema(description = "Age of the customer", required = true, example = "1")
    @Column(name = "age", nullable = false)
    private Integer age;

    @JsonManagedReference
    @JoinColumn(name = "fk_shipping_address_id")
    @OneToOne(cascade=CascadeType.ALL)
    private ShippingAddress shippingAddress;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", shippingAddress=" + shippingAddress +
                '}';
    }
}
