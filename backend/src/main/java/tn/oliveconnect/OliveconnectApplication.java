package tn.oliveconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import tn.oliveconnect.security.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class OliveconnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(OliveconnectApplication.class, args);
	}

}
