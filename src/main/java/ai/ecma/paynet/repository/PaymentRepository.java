package ai.ecma.paynet.repository;

import ai.ecma.paynet.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findFirstByTransactionIdOrderByPayDateDesc(Long transactionId);


}
