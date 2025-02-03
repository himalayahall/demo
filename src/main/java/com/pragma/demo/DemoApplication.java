package com.pragma.demo;

import java.lang.management.ManagementFactory;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoApplication {

	public static void main(String[] args) {
		// foo();
		// logJvmArguments();
		SpringApplication.run(DemoApplication.class, args);
	}

	public static void logJvmArguments() {

		System.out.println("Java Version: " + System.getProperty("java.version"));
		System.out.println("Java Home: " + System.getProperty("java.home"));

		System.out.println(System.getProperty("java.vm.args"));

		// Log JVM startup arguments
		List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
		System.out.println("JVM Startup Parameters:");
		jvmArgs.forEach(System.out::println);

		// Log actual heap memory settings
		long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024); // Convert to MB
		long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
		long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);

		System.out.println("===== JVM MEMORY SETTINGS =====");
		System.out.println("Max Heap Size (-Xmx): " + maxMemory + " MB");
		System.out.println("Total Heap Size: " + totalMemory + " MB");
		System.out.println("Free Heap Size: " + freeMemory + " MB");
	}
}
