package ai.ecma.paynet.repository;

import ai.ecma.paynet.entity.PaynetTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface PaynetTransactionRepository extends JpaRepository<PaynetTransaction, Integer> {

    List<PaynetTransaction> findAllByCreatedAtBetween(Timestamp createdAt, Timestamp createdAt2);

    Optional<PaynetTransaction> findByTransactionId(long transactionId);

    boolean existsByTransactionId(long transactionId);

}
