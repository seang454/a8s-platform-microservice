package co.istad.a8sclustermain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"co.istad"})
public class A8sClusterMainApplication {

    public static void main(String[] args) {
        SpringApplication.run(A8sClusterMainApplication.class, args);
    }
}
