package com.ling.ping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.channels.FileLock;

/**
 * Imaging a simplified scenario where you are asked to implement 2 microservices called “ping” and “pong” respectively and integrates with each other as illustrated below –
 *
 *
 *
 *
 *
 * Both services must be implemented using Spring WebFlux and can be packaged & run as executable jar.
 * Both services are running locally in same device. The integration goes as simple as – Ping Service attempts to says “Hello” to Pong Service around every 1 second and then Pong service should respond with “World”.
 * The Pong Service should be implemented with a Throttling Control limited with value 1, meaning that –
 * For any given second, there is only 1 requests can be handled by it.
 * For those additional requests coming in the given second, Pong Service should return 429 status code.
 * Multiple Ping Services should be running as separate JVM Process with capability of Rate Limit Control across all processes with only 2 RPS (hint: consider using Java FileLock), meaning that -
 * If all processes attempt to triggers Pong Service at the same time, only 2 requests are allowed to go out to Pong Service.
 * Among the 2 outgoing requests to Pong, if they reach Pong Service within the same second, one of them are expected to be throttled with 429 status code.
 * Each Ping service process must log the request attempt with result (*)  in separate logs per instance for review. The result includes:
 * Request sent & Pong Respond.
 * Request not send as being “rate limited”.
 * Request send & Pong throttled it.
 * Increase the number of running Ping processes locally and review the logs for each.
 *
 *
 * Code Quality Acceptance Criteria:
 *
 * Using Spring Boot and Spring Webflux Framework is a must.
 * Using Spring Spock Framework in Groovy for Unit Test.
 * Unit Test with Coverage >= 80%. (hint: Maven Jacoco Plugin should be used).
 */
@SpringBootApplication
@EnableScheduling
public class PingServiceApplication {
    private final WebClient webClient = WebClient.create("http://localhost:8081");
    // 每个JVM进程每次最大请求数
    private static final int MAX_REQUESTS_PER_SECOND = 2;

    public static void main(String[] args) {
        SpringApplication.run(PingServiceApplication.class, args);
    }


    @Scheduled(fixedRate = 1000)
    public void sendPing() throws IOException {
        Long pid = getPid();
        System.out.println("当前进程pid = " + pid);
        String lockFilePath = System.getProperty("java.io.tmpdir") + "/tempFile.txt";
        //加进程锁
        try (RandomAccessFile file = new RandomAccessFile(lockFilePath, "rw");
             FileLock lock = file.getChannel().tryLock()) {
            if (lock != null) {
                //获取到文件锁的进程发送2次ping请求到pong服务
                for (int i = 0; i < MAX_REQUESTS_PER_SECOND; i++) {
                    sendHello();
                }
            } else {
                System.out.println("Request not sent as being 'rate limited'");
            }
        }
    }

    private Long getPid() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        String name = runtimeMXBean.getName();
        long pid = Long.parseLong(name.split("@")[0]);
        return pid;
    }

    private void sendHello() {
        webClient.get().uri("/pong")
                .retrieve()
                .toBodilessEntity()
                //解析异步响应结果
                .subscribe(
                        response -> System.out.println("Request sent & Pong Respond: " + response.getStatusCode()),
                        error -> {
                            if (error.getMessage().contains("429")) {
                                System.out.println("Pong throttled it.");
                            } else {
                                System.out.println("Request failed: " + error.getMessage());
                            }
                        }
                );
    }

}
