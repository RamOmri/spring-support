package com.nullify.supportportal.repository;

import com.nullify.supportportal.domain.Ticket;
import com.nullify.supportportal.domain.TicketStatus;
import com.nullify.supportportal.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByCustomerOrderByCreatedAtDesc(User customer);

    List<Ticket> findAllByOrderByCreatedAtDesc();

    List<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status);

    @Query(value = "SELECT * FROM tickets WHERE title ILIKE :pattern OR description ILIKE :pattern "
            + "ORDER BY created_at DESC LIMIT 100",
            nativeQuery = true)
    List<Ticket> searchByPattern(@Param("pattern") String pattern);

    @Query(value = "SELECT t.* FROM tickets t WHERE (t.title ILIKE :pattern OR t.description ILIKE :pattern) "
            + "AND t.customer_id = :customerId ORDER BY t.created_at DESC LIMIT 100",
            nativeQuery = true)
    List<Ticket> searchByPatternForCustomer(@Param("pattern") String pattern,
                                            @Param("customerId") long customerId);
}
