package com.xiaomi.hera.trace.etl.consumer;

import com.xiaomi.hera.trace.etl.api.SpanHoldService;
import com.xiaomi.hera.trace.etl.domain.metrics.SpanHolder;
import com.xiaomi.hera.tspandata.TSpanData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "service.selector.property", havingValue = "outer")
public class SpanHoldServiceImpl implements SpanHoldService {

    @Override
    public SpanHolder getSpanHolder(TSpanData data) {
        SpanHolder spanHolder = new SpanHolder(data);
        String service = spanHolder.getService();
        if(service == null){
            return null;
        }
        spanHolder.setApplication(service.replace("-", "_"));
        return spanHolder;
    }
}