package com.amalia.harmonyhub_backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableCaching
public class HarmonyhubBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HarmonyhubBackendApplication.class, args);
	}
	@Bean(name = "taskExecutor")
	public TaskExecutor taskExecutor() {
		return new SimpleAsyncTaskExecutor();
	}
}
