/*
 * Copyright 2020 Xiaomi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.xiaomi.hera.trace.etl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @Description To get config from a non-Spring management class, the configuration is set in when springboot starts
 */
@Configuration("envConfig")
public class EnvConfig {

    @Value("${server.type}")
    private String serverType;
    @Value("${es.error.index}")
    private String errorTraceIndexPrefix;
    @Value("${es.trace.index.driver.prefix}")
    private String driverTraceIndexPrefix;

    public static String ERROR_TRACE_INDEX_PREFIX;

    public static String DRIVER_TRACE_INDEX_PREFIX;

    public static String SERVER_TYPE;

    @PostConstruct
    public void init() {
        ERROR_TRACE_INDEX_PREFIX = errorTraceIndexPrefix;
        DRIVER_TRACE_INDEX_PREFIX = driverTraceIndexPrefix;
        SERVER_TYPE = serverType;
    }
}