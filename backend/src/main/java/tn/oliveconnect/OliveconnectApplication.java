package tn.oliveconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import tn.oliveconnect.security.JwtProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
public class OliveconnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(OliveconnectApplication.class, args);
	}

}
