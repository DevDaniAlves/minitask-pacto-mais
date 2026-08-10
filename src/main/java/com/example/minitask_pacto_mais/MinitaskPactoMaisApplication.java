package com.example.minitask_pacto_mais;

import com.example.minitask_pacto_mais.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MinitaskPactoMaisApplication {

	public static void main(String[] args) {
		DotEnvLoader.load();
		SpringApplication.run(MinitaskPactoMaisApplication.class, args);
	}

}
