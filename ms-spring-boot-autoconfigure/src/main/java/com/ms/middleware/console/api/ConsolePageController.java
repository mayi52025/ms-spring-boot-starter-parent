package com.ms.middleware.console.api;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 控制台页面：返回 classpath:static/ms-console/index.html。
 * 固定映射 /ms-console；API 路径可通过 base-path 配置（见 AutonomyConsoleController）。
 */
@RestController
@RequestMapping("/ms-console")
public class ConsolePageController {

    @GetMapping(value = {"", "/", "/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> index() throws IOException {
        Resource resource = new ClassPathResource("static/ms-console/index.html");
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(content);
    }
}
