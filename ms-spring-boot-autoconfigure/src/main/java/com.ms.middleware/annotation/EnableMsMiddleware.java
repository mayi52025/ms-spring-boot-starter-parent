package com.ms.middleware.annotation;

import com.ms.middleware.MsMiddlewareAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(MsMiddlewareAutoConfiguration.class)
@Documented
public @interface EnableMsMiddleware {
}
