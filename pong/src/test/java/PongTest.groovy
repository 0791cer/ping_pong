package com.ling.pong

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import spock.lang.Specification

import java.util.concurrent.locks.ReentrantLock

import static org.springframework.web.reactive.function.server.RequestPredicates.GET
import static org.springframework.web.reactive.function.server.RouterFunctions.route

@WebFluxTest(PongServiceApplication)
class PongTest extends Specification {

    @Autowired
    private WebTestClient webTestClient

    def "return world"() {
        expect:
        webTestClient.get().uri("/pong")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("World")
    }

    def "return too many request"() {
        given:
        def lock = new ReentrantLock()
        def application = new PongServiceApplication() {
            @Override
            public RouterFunction<ServerResponse> pongRoute() {
                return route(GET("/pong"), request -> {
                    boolean hasLock = lock.tryLock()
                    if (hasLock) {
                        println("World")
                        return ServerResponse.ok().bodyValue("World")
                                .doFinally(signal -> lock.unlock())
                    } else {
                        println("Too Many Requests")
                        return ServerResponse.status(429).bodyValue("Too Many Requests")
                    }
                })
            }
        }
        def routerFunction = application.pongRoute()
        def webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build()

        when:
        lock.lock()
        def response = webTestClient.get().uri("/pong").exchange()

        then:
        response.expectStatus().isEqualTo(429)
        response.expectBody(String.class).isEqualTo("Too Many Requests")

        cleanup:
        lock.unlock()
    }
}
