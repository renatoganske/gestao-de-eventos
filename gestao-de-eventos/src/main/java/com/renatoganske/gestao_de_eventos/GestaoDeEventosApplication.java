package com.renatoganske.gestao_de_eventos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestaoDeEventosApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestaoDeEventosApplication.class, args);
	}

}
