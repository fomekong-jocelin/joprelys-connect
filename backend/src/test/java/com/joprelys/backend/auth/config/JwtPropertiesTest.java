package com.joprelys.backend.auth.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class JwtPropertiesTest {

	private static final String PUBLISHED_SECRET =
			"snMVSBygC5IUa+4aF3dOcjZ/UVlmb8UIYbfu7dFKu2SE6kZ/mJUg5fsNMbS1MCcfwU1XiqR1OA5Tga+UpPwcPA==";

	@Test
	void shouldRefuseSecretPublishedInRepositoryHistory() {
		assertThrows(IllegalStateException.class,
				() -> new JwtProperties("issuer", "audience", PUBLISHED_SECRET, 15));
	}

	@Test
	void shouldAcceptFreshSecret() {
		assertDoesNotThrow(
				() -> new JwtProperties("issuer", "audience", "a-fresh-secret-that-is-long-enough-123", 15));
	}

	@Test
	void devProfileDrawsARandomSecretInsteadOfAVersionedOne() throws IOException {
		String first = devProfileSecret();
		String second = devProfileSecret();

		assertEquals(64, first.length());
		assertNotEquals(first, second);
		assertDoesNotThrow(() -> new JwtProperties("issuer", "audience", first, 15));
	}

	private static String devProfileSecret() throws IOException {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		new YamlPropertySourceLoader().load("dev", new ClassPathResource("application-dev.yml"))
				.forEach(environment.getPropertySources()::addLast);
		RandomValuePropertySource.addToEnvironment(environment);
		return environment.getProperty("joprelys.security.jwt.secret");
	}
}
