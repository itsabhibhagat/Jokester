package com.application.jokester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

// EnableSpringDataWebSupport fixes the PageImpl serialization warning.
// When searchJokes returns a Page, Spring now serializes it as a stable
// PagedModel JSON structure instead of the raw PageImpl which has no
// guaranteed JSON stability across Spring versions.
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class JokesterApplication {
	public static void main(String[] args) {
		SpringApplication.run(JokesterApplication.class, args);
	}
}