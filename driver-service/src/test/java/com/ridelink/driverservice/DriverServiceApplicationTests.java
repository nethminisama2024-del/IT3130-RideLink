package com.ridelink.driverservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class DriverServiceApplicationTests {
	@DynamicPropertySource
	static void testProperties(DynamicPropertyRegistry registry) {
		registry.add("jwt.secret", () -> java.util.UUID.randomUUID().toString()
				+ java.util.UUID.randomUUID());
		registry.add("ride.service.key", () -> java.util.UUID.randomUUID().toString());
		registry.add("spring.datasource.url", () -> "jdbc:h2:mem:driver_context_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
		registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
		registry.add("spring.datasource.username", () -> "sa");
		registry.add("spring.datasource.password", () -> "");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
	}

	@Test
	void contextLoads() {
	}

}
