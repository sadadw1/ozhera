/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ozhera.demo.client.controller;

import com.xiaomi.hera.trace.annotation.Trace;
import org.apache.ozhera.demo.client.api.service.DubboHealthService;
import org.apache.ozhera.demo.client.grpc.GrpcClientService;
import org.apache.ozhera.demo.client.util.HttpClientUtil;
import org.apache.ozhera.prometheus.all.client.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import run.mone.common.Result;

import javax.annotation.PostConstruct;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class TestController {

    @Autowired
    private DubboHealthService dubboHealthService;
    @Autowired
    private GrpcClientService grpcClientService;
    @Autowired
    private JedisPool jedisPooled;

    @PostConstruct
    public void init(){
        new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(
                () -> {
                    sendGETReuqest("http://localhost:8085/testok");
                    sendGETReuqest("http://localhost:8085/remotehealth?size=1");
                    sendGETReuqest("http://localhost:8085/remotehealth2");
                    sendGETReuqest("http://localhost:8085/testError");
                    sendGETReuqest("http://localhost:8085/customizedMetrics");
                    sendGETReuqest("http://localhost:8085/grpcTest");
                },
                0,
                15,
                TimeUnit.SECONDS);
    }

    private CloseableHttpClient client = HttpClientUtil.getInstance().getHttpClient();

    @GetMapping("/jedisTest")
    public String jedisTest() {
        try (Jedis jedis = jedisPooled.getResource()) {
            jedis.get("aaaa");
            jedis.get("bbbb");
            jedis.get("cccc");
        } catch (Exception e) {
            log.error("redis exception : ", e);
        }
        return "ok";
    }

    @GetMapping("/testok")
    public Object testok() {
        return Result.success("ok");
    }


    @GetMapping("/remotehealth")
    public Object remotehealth() {
        dubboHealthService.remoteHealth(1);
        return "ok";
    }

    @GetMapping("/remotehealth2")
    public Object remotehealth2() {
        dubboHealthService.remoteHealth2();
        return "ok";
    }

    @GetMapping("/testError")
    public Object testError() {
        throw new RuntimeException("test error");
    }

    @GetMapping("/testResultCode500")
    public Object testResultCode500() {
        return dubboHealthService.testResultCode500();
    }

    double[] buckets = new double[]{0.01, 0.1, 1.0, 5.0, 10.0, 20.0, 40.0, 80.0, 200.0, 300.0, 400.0, 600.0, 800.0, 1000.0,2000.0,3000.0};

    @GetMapping("/customizedMetrics")
    public Object customizedMetrics() {
        Metrics.getInstance().newCounter("test_counter").add(1);
        long l = System.currentTimeMillis();
        try {
            TimeUnit.MILLISECONDS.sleep(20);
        } catch (InterruptedException e) {
        }
        long duration = System.currentTimeMillis() - l;
        Metrics.getInstance().newHistogram("test_histogram",buckets).observe(duration);
        Metrics.getInstance().newGauge("test_gauge").set(duration);
        return "ok";
    }

    @GetMapping("/grpcTest")
    public Object grpcTest() {
        grpcClientService.grpcNormal("ok");
        grpcClientService.grpcSlow("ok");
        grpcClientService.grpcError("ok");
        return "ok";
    }

    @Trace
    private String sendGETReuqest(String url) {
        try {
            RequestBuilder requestBuilder = RequestBuilder.get(url);
            requestBuilder.setHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8").setConfig(HttpClientUtil.getInstance().getRequestConfig());
            HttpUriRequest httpUriRequest = requestBuilder.build();
            String responseBody = null;
            CloseableHttpResponse response = null;
            int statusCode = 0;
            try {
                response = client.execute(httpUriRequest);
                statusCode = response.getStatusLine().getStatusCode();
                responseBody = EntityUtils.toString(response.getEntity(), "utf-8");
                log.info("http client status : " + statusCode);
                return responseBody;
            } catch (Exception e) {
                log.error("http client execute error",e);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        } catch (Exception e) {
            log.error("http client error",e);
        }
        return null;
    }

}