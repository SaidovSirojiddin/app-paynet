package ai.ecma.paynet.component;

import ai.ecma.paynet.entity.Client;
import ai.ecma.paynet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("Paynet")) {
            userRepository.save(new Client(
                    "Paynet",
                    passwordEncoder.encode("PaynetUchunParolEdiBu")
            ));
            userRepository.save(new Client(
                    "+998001234567",
                    passwordEncoder.encode("parolClient")
            ));
        }
    }
}
