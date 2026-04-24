package com.application.jokester.config.datasource;

import java.lang.annotation.*;

// Custom annotation to mark methods that should use replica
// Usage: just add @ReadOnly on any service method that only reads data
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnly {
}