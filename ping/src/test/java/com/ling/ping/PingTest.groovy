package com.ling.ping

import com.ling.ping.PingServiceApplication;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;
import spock.lang.Specification

import java.lang.reflect.Field;

@ExtendWith(SpringExtension.class)
@WebFluxTest(PingServiceApplication.class)
class PingTest extends Specification {

    @Autowired
    PingServiceApplication application;

    @MockBean
    private WebClient.Builder webClientBuilder;

    private WebClient webClient;

    def setup() {
        // 使用 MockBean 的 WebClient.Builder 来创建一个模拟的 WebClient
        webClient = webClientBuilder.build();
    }

    def "Test sendHello method with successful request"() {
        given:
        def response = ClientResponse.create(HttpStatus.OK).build()
        webClient.get().uri("localhost:8081/pong").retrieve() >> Mono.just(response)

        when:
        new PingServiceApplication().sendHello()

        then:
        1 * webClient.get().uri("localhost:8081/pong").retrieve() >> {
            thenReturn(Mono.just(response))
        }
    }

    def "Test sendHello method with throttled request"() {
        given:
        def response = ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS).build();
        webClient.get().uri("localhost:8081/pong").retrieve() >> Mono.just(response);

        when:
        application.sendHello();

        then:
        1 * webClient.get().uri("localhost:8081/pong").retrieve();
    }

    def "Test sendHello method with failed request"() {
        given:
        webClient.get().uri("localhost:8081/pong").retrieve() >> Mono.error(new RuntimeException("Request failed"));

        when:
        application.sendHello();

        then:
        1 * webClient.get().uri("localhost:8081/pong").retrieve();
    }
}
