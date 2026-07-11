package com.ms.middleware.health;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Redis 健康检查器：优先用原生 PING 探活，不依赖可能已损坏的 Redisson 连接池。
 */
public class RedisHealthChecker implements HealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthChecker.class);
    private static final int PROBE_TIMEOUT_SECONDS = 3;

    private final AtomicReference<RedissonClient> redissonClientRef;
    private final String host;
    private final int port;
    private final String password;

    public RedisHealthChecker(AtomicReference<RedissonClient> redissonClientRef,
                              String host,
                              int port,
                              String password) {
        this.redissonClientRef = redissonClientRef;
        this.host = host;
        this.port = port;
        this.password = password;
    }

    @Override
    public boolean checkHealth() {
        if (redisPing()) {
            return true;
        }
        return probeRedissonClient();
    }

    /** 直连 Redis 协议 PING，与 Redisson 连接池状态解耦 */
    private boolean redisPing() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            if (StringUtils.hasText(password)) {
                writeCommand(out, "AUTH", password);
                String authResp = readLine(in);
                if (authResp == null || authResp.startsWith("-")) {
                    logger.warn("Redis AUTH failed: {}", authResp);
                    return false;
                }
            }
            writeCommand(out, "PING");
            String resp = readLine(in);
            return resp != null && resp.contains("PONG");
        } catch (Exception e) {
            logger.debug("Redis PING failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean probeRedissonClient() {
        try {
            RedissonClient client = redissonClientRef.get();
            if (client == null || client.isShutdown()) {
                return false;
            }
            client.getKeys().countAsync().get(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            logger.debug("Redisson probe failed: {}", e.getMessage());
            return false;
        }
    }

    private static void writeCommand(OutputStream out, String... parts) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append('*').append(parts.length).append("\r\n");
        for (String part : parts) {
            sb.append('$').append(part.length()).append("\r\n").append(part).append("\r\n");
        }
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static String readLine(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                in.read();
                break;
            }
            sb.append((char) b);
        }
        return sb.toString();
    }
}
