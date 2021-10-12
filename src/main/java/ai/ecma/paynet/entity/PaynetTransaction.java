package ai.ecma.paynet.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaynetTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long amount;

    private Long serviceId;

    @Column(unique = true)
    private Long transactionId;

    private Timestamp transactionTime;

    private String phoneNumber;

    private Integer state;

    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    public PaynetTransaction(Long amount, Long serviceId, Long transactionId, Timestamp transactionTime, String phoneNumber, Integer state) {
        this.amount = amount;
        this.serviceId = serviceId;
        this.transactionId = transactionId;
        this.transactionTime = transactionTime;
        this.phoneNumber = phoneNumber;
        this.state = state;
    }
}
