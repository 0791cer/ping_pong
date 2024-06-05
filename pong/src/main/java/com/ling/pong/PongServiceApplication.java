package com.ling.pong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class PongServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PongServiceApplication.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> pongRoute() {
        Lock lock=new ReentrantLock();

        return route(GET("/pong"), request -> {
            boolean hasLock = lock.tryLock();
            if (hasLock) {
                System.out.println("World");
                return ServerResponse.ok().bodyValue("World")
                        .doFinally(signal -> lock.unlock());
            } else {
                System.out.println("Too Many Requests");
                return ServerResponse.status(429).bodyValue("Too Many Requests");
            }
        });
    }
}
