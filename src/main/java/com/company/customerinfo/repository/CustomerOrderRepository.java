package com.company.customerinfo.repository;

import com.company.customerinfo.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Integer> {

    @Query("""
            SELECT DISTINCT o FROM CustomerOrder o
            LEFT JOIN FETCH o.orderItems
            LEFT JOIN FETCH o.customer c
            LEFT JOIN FETCH c.shippingAddress
            """)
    List<CustomerOrder> findAllWithAssociations();

    @Query("""
            SELECT DISTINCT o FROM CustomerOrder o
            LEFT JOIN FETCH o.orderItems
            LEFT JOIN FETCH o.customer c
            LEFT JOIN FETCH c.shippingAddress
            WHERE o.id = :id
            """)
    Optional<CustomerOrder> findByIdWithAssociations(@Param("id") Integer id);
}

