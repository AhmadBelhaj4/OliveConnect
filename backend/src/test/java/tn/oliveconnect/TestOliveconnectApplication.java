package tn.oliveconnect;

import org.springframework.boot.SpringApplication;

public class TestOliveconnectApplication {

	public static void main(String[] args) {
		SpringApplication.from(OliveconnectApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
