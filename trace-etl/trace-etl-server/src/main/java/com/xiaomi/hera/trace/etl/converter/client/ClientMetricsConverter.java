package com.xiaomi.hera.trace.etl.converter.client;

import com.xiaomi.hera.trace.etl.consumer.MultiMetricsCall;
import com.xiaomi.hera.trace.etl.domain.converter.ClientConverter;
import com.xiaomi.hera.trace.etl.domain.metrics.MetricsBucket;
import com.xiaomi.hera.trace.etl.converter.BaseMetricsConverter;
import com.xiaomi.hera.trace.etl.source.ErrorSourceReceive;
import com.xiaomi.hera.trace.etl.source.service.SourceObtainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.defaultString;

@Service
@Slf4j
public class ClientMetricsConverter extends BaseMetricsConverter {

    @Autowired
    private ErrorSourceReceive errorSourceReceive;

    @Autowired
    private SourceObtainService sourceObtainService;

    public void convert(ClientConverter clientConverter) {
        String type;
        switch (clientConverter.getSpanType()) {
            case http:
                String[] httpKeys = tagKeys("serviceName", "methodName");
                String[] httpValues = tagValues(clientConverter, clientConverter.getServiceName(), clientConverter.getMethodName());
                String[] httpKeysWithCode = tagKeys("serviceName", "methodName", "errorCode");
                String[] httpValuesWithCode = tagValues(clientConverter, clientConverter.getServiceName(), clientConverter.getMethodName(), String.valueOf(clientConverter.getResponseCode()));
                type = "aop";
                multiMetricsCall.newCounter(buildMetricName(type, "ClientTotalMethodCount"), httpKeys).with(httpValues).add(1, httpValues);
                multiMetricsCall.newHistogram(buildMetricName(type, "ClientMethodTimeCount"), MetricsBucket.HTTP_BUCKET, httpKeys).with(httpValues).observe(clientConverter.getDuration(), httpValues);
                if (clientConverter.isError()) {
                    multiMetricsCall.newCounter(buildMetricName("http", "ClientError"), httpKeysWithCode).with(httpValuesWithCode).add(1, httpValuesWithCode);
                    errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getErrorTraceSourceDomain(clientConverter));
                } else {
                    multiMetricsCall.newCounter(buildMetricName(type, "SuccessMethodCount"), httpKeys).with(httpValues).add(1, httpValues);
                    if (clientConverter.getDuration() > getSlowThreshold(clientConverter.getSpanType(), clientConverter.getApplication())) {
                        multiMetricsCall.newCounter(buildMetricName("http", "SlowQuery"), httpKeys).with(httpValues).add(1, httpValues);
                        errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getSlowTraceSourceDomain(clientConverter));
                    }
                }
                break;
            case dubbo:
                type = "dubbo";
                String[] dubboKeys = tagKeys("serviceName", "methodName");
                String[] dubboValues = tagValues(clientConverter, clientConverter.getServiceName(), clientConverter.getMethodName());
                multiMetricsCall.newCounter(buildMetricName(type, "BisTotalCount"), dubboKeys).with(dubboValues).add(1, dubboValues);
                multiMetricsCall.newHistogram(buildMetricName(type, "ConsumerTimeCost"), MetricsBucket.DUBBO_BUCKET, dubboKeys).with(dubboValues).observe(clientConverter.getDuration(), dubboValues);
                if (clientConverter.isError()) {
                    multiMetricsCall.newCounter(buildMetricName(type, "ConsumerError"), dubboKeys).with(dubboValues).add(1, dubboValues);
                    errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getErrorTraceSourceDomain(clientConverter));
                } else {
                    multiMetricsCall.newCounter(buildMetricName(type, "BisSuccessCount"), dubboKeys).with(dubboValues).add(1, dubboValues);
                    if (clientConverter.getDuration() > getSlowThreshold(clientConverter.getSpanType(), clientConverter.getApplication())) {
                        multiMetricsCall.newCounter(buildMetricName(type, "ConsumerSlowQuery"), dubboKeys).with(dubboValues).add(1, dubboValues);
                        errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getSlowTraceSourceDomain(clientConverter));
                    }
                }
                break;
            case redis:
                type = "Redis";
                String[] redisKeys = tagKeys("host", "port", "method");
                Map<String, String> result = parseDsn(clientConverter.getServiceName());
                String[] redisValues = tagValues(clientConverter, defaultString(result.get("host")), defaultString(result.get("port")), clientConverter.getMethodName());

                multiMetricsCall.newCounter(buildMetricName(type, "TotalCount"), redisKeys).with(redisValues).add(1, redisValues);
                multiMetricsCall.newHistogram(buildMetricName(type, "MethodTimeCost"), MetricsBucket.REDIS_BUCKET, redisKeys).with(redisValues).observe(clientConverter.getDuration(), redisValues);
                if (clientConverter.isError()) {
                    multiMetricsCall.newCounter(buildMetricName("redis", "Error"), redisKeys).with(redisValues).add(1, redisValues);
                    errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getErrorTraceSourceDomain(clientConverter));
                } else {
                    multiMetricsCall.newCounter(buildMetricName(type, "SuccessCount"), redisKeys).with(redisValues).add(1, redisValues);
                    if (clientConverter.getDuration() > getSlowThreshold(clientConverter.getSpanType(), clientConverter.getApplication())) {
                        multiMetricsCall.newCounter(buildMetricName("redis", "SlowQuery"), redisKeys).with(redisValues).add(1, redisValues);
                        errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getSlowTraceSourceDomain(clientConverter));
                    }
                }
                break;
            case mysql:
                type = "sql";
                String[] sqlKeys = tagKeys("dataSource", "sqlMethod", "sql");
                String[] sqlValues = tagValues(clientConverter, clientConverter.getDataSource(), clientConverter.getMethodName(), clientConverter.getSql());
                multiMetricsCall.newCounter(buildMetricName(type, "TotalCount"), sqlKeys).with(sqlValues).add(1, sqlValues);
                multiMetricsCall.newHistogram(buildMetricName(type, "TotalTimer"), MetricsBucket.SQL_BUCKET, sqlKeys).with(sqlValues).observe(clientConverter.getDuration(), sqlValues);
                if (clientConverter.isError()) {
                    multiMetricsCall.newCounter(buildMetricName("db", "Error"), sqlKeys).with(sqlValues).add(1, sqlValues);
                    errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getErrorTraceSourceDomain(clientConverter));
                } else {
                    multiMetricsCall.newCounter(buildMetricName(type, "SuccessCount"), sqlKeys).with(sqlValues).add(1, sqlValues);
                    if (clientConverter.getDuration() > getSlowThreshold(clientConverter.getSpanType(), clientConverter.getApplication())) {
                        multiMetricsCall.newCounter(buildMetricName("db", "SlowQuery"), sqlKeys).with(sqlValues).add(1, sqlValues);
                        errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getSlowTraceSourceDomain(clientConverter));
                    }
                }
                break;
            case elasticsearch:
            case mongodb:
                type = clientConverter.getSpanType().name() + "Client";
                String[] dbKeys = tagKeys("dataSource", "sqlMethod", "sql");
                String[] dbValues = tagValues(clientConverter, clientConverter.getDataSource(), clientConverter.getMethodName(), clientConverter.getSql());
                multiMetricsCall.newCounter(buildMetricName(type), dbKeys).with(dbValues).add(1, dbValues);
                multiMetricsCall.newHistogram(buildMetricName(type, "TimeCost"), MetricsBucket.SQL_BUCKET, dbKeys).with(dbValues).observe(clientConverter.getDuration(), dbValues);
                if (clientConverter.isError()) {
                    multiMetricsCall.newCounter(buildMetricName(type, "Error"), dbKeys).with(dbValues).add(1, dbValues);
                    errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getErrorTraceSourceDomain(clientConverter));
                } else {
                    multiMetricsCall.newCounter(buildMetricName(type, "Success"), dbKeys).with(dbValues).add(1, dbValues);
                    if (clientConverter.getDuration() > getSlowThreshold(clientConverter.getSpanType(), clientConverter.getApplication())) {
                        multiMetricsCall.newCounter(buildMetricName(type, "SlowQuery"), dbKeys).with(dbValues).add(1, dbValues);
                        errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getSlowTraceSourceDomain(clientConverter));
                    }
                }
                break;
            case kafka:
            case rocketmq:
                type = clientConverter.getSpanType().name() + "Producer";
                String[] mqKeys = tagKeys("topic", "method");
                String[] mqValues = tagValues(clientConverter, clientConverter.getTopic(), clientConverter.getMethodName());
                multiMetricsCall.newCounter(buildMetricName(type), mqKeys).with(mqValues).add(1, mqValues);
                multiMetricsCall.newHistogram(buildMetricName(type, "TimeCost"), MetricsBucket.MQ_BUCKET, mqKeys).with(mqValues).observe(clientConverter.getDuration(), mqValues);
                if (clientConverter.isError()) {
                    multiMetricsCall.newCounter(buildMetricName(type, "Error"), mqKeys).with(mqValues).add(1, mqValues);
                    errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getErrorTraceSourceDomain(clientConverter));
                } else {
                    multiMetricsCall.newCounter(buildMetricName(type, "Success"), mqKeys).with(mqValues).add(1, mqValues);
                    if (clientConverter.getDuration() > getSlowThreshold(clientConverter.getSpanType(), clientConverter.getApplication())) {
                        multiMetricsCall.newCounter(buildMetricName(type, "SlowProduce"), mqKeys).with(mqValues).add(1, mqValues);
                        errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getSlowTraceSourceDomain(clientConverter));
                    }
                }
                break;
            case oracle:
                type = "oracle";
                String[] oracleKeys = tagKeys("dataSource", "sqlMethod", "sql");
                String[] oracleValues = tagValues(clientConverter, clientConverter.getDataSource(), clientConverter.getMethodName(), clientConverter.getSql());
                multiMetricsCall.newCounter(buildMetricName(type, "TotalCount"), oracleKeys).with(oracleValues).add(1, oracleValues);
                multiMetricsCall.newHistogram(buildMetricName(type, "TotalTimer"), MetricsBucket.SQL_BUCKET, oracleKeys).with(oracleValues).observe(clientConverter.getDuration(), oracleValues);
                if (clientConverter.isError()) {
                    multiMetricsCall.newCounter(buildMetricName(type, "Error"), oracleKeys).with(oracleValues).add(1, oracleValues);
                    errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getErrorTraceSourceDomain(clientConverter));
                } else {
                    multiMetricsCall.newCounter(buildMetricName(type, "SuccessCount"), oracleKeys).with(oracleValues).add(1, oracleValues);
                    if (clientConverter.getDuration() > getSlowThreshold(clientConverter.getSpanType(), clientConverter.getApplication())) {
                        multiMetricsCall.newCounter(buildMetricName(type, "SlowQuery"), oracleKeys).with(oracleValues).add(1, oracleValues);
                        errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getSlowTraceSourceDomain(clientConverter));
                    }
                }
                break;
            case grpc:
            case thrift:
            case apus:
                type = clientConverter.getSpanType().name() + "Client";
                String[] rpcKeys = tagKeys("serviceName", "methodName");
                String[] rpcValues = tagValues(clientConverter, clientConverter.getServiceName(), clientConverter.getMethodName());
                multiMetricsCall.newCounter(buildMetricName(type), rpcKeys).with(rpcValues).add(1, rpcValues);
                multiMetricsCall.newHistogram(buildMetricName(type, "TimeCost"), MetricsBucket.DUBBO_BUCKET, rpcKeys).with(rpcValues).observe(clientConverter.getDuration(), rpcValues);
                if (clientConverter.isError()) {
                    multiMetricsCall.newCounter(buildMetricName(type, "Error"), rpcKeys).with(rpcValues).add(1, rpcValues);
                } else {
                    multiMetricsCall.newCounter(buildMetricName(type, "Success"), rpcKeys).with(rpcValues).add(1, rpcValues);
                    if (clientConverter.getDuration() > getSlowThreshold(clientConverter.getSpanType(), clientConverter.getApplication())) {
                        multiMetricsCall.newCounter(buildMetricName(type, "SlowQuery"), rpcKeys).with(rpcValues).add(1, rpcValues);
                        errorSourceReceive.submitErrorTraceDomain(sourceObtainService.getSlowTraceSourceDomain(clientConverter));
                    }
                }
                break;
        }
        // extension
        metricsExtend(clientConverter);
    }
}