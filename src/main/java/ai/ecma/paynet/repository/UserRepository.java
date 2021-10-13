package ai.ecma.paynet.repository;

import ai.ecma.paynet.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Integer> {

    Optional<Client> findByUsername(String username);

    boolean existsByUsername(String username);
}
