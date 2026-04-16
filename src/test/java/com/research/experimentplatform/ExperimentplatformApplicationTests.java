package com.research.experimentplatform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
	"spring.datasource.url=jdbc:h2:mem:testdb",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"supabase.url=https://placeholder.supabase.co",
	"cors.allowed-origins=http://localhost:3000"
})
class ExperimentplatformApplicationTests {

	@Test
	void contextLoads() {
	}

}
