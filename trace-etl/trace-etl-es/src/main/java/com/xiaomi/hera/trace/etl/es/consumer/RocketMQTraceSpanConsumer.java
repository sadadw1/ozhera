package com.xiaomi.hera.trace.etl.es.consumer;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.xiaomi.hera.trace.etl.api.service.MQExtension;
import com.xiaomi.hera.trace.etl.bo.MqConfig;
import com.xiaomi.hera.trace.etl.es.util.pool.ConsumerPool;
import com.xiaomi.hera.trace.etl.util.ThriftUtil;
import com.xiaomi.hera.tspandata.TSpanData;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.thrift.TDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author dingtao
 * @Date 2021/11/5 10:05 am
 */
@Service
@ConditionalOnProperty(name = "mq", havingValue = "rocketMQ")
@Slf4j
public class RocketMQTraceSpanConsumer {

    @Value("${mq.consumer.group}")
    private String group;

    @NacosValue("${mq.nameseraddr}")
    private String nameSerAddr;

    @NacosValue("${mq.es.topic}")
    private String topicName;

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    private MQExtension mq;

    @PostConstruct
    public void takeMessage() throws MQClientException {
        MqConfig<MessageExt> config = new MqConfig<>();
        config.setNameSerAddr(nameSerAddr);

        config.setConsumerGroup(group);
        config.setConsumerTopicName(topicName);

        config.setBatchConsumerMethod((list)->{
            try {
                for(MessageExt message : list) {
                    ConsumerPool.CONSUMER_POOL.submit(new ConsumerRunner(message.getBody()));
                    await();
                }
            } catch (Throwable t) {
                log.error("consumer message error", t);
            }
            return true;
        });

        mq.initMq(config);
    }

    private void await() {
        while (true) {
            try {
                if (ConsumerPool.CONSUMER_QUEUE.remainingCapacity() > ConsumerPool.CONSUMER_QUEUE_THRESHOLD) {
                    return;
                }
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (Throwable t) {
                log.error("await error : ", t);
            }
        }
    }

    private class ConsumerRunner implements Runnable {
        private byte[] message;

        public ConsumerRunner(byte[] message) {
            this.message = message;
        }

        @Override
        public void run() {
            try {
                TSpanData tSpanData = new TSpanData();
                new TDeserializer(ThriftUtil.PROTOCOL_FACTORY).deserialize(tSpanData, message);
                consumerService.consumer(tSpanData);
            } catch (Throwable t) {
                log.error("consumer error : ", t);
            }
        }
    }
}
