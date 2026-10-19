package com.application.jokester;

import com.application.jokester.event.DonationEvent;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
class JokesterApplicationTests {

    @MockBean
    private LettuceBasedProxyManager<String> proxyManager;

    @MockBean
    private KafkaTemplate<String, DonationEvent> kafkaTemplate;

    @Test
    void contextLoads() {
    }
}
