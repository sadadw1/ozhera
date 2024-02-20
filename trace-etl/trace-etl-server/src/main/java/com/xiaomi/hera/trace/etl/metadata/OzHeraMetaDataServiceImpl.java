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
package com.xiaomi.hera.trace.etl.metadata;

import com.xiaomi.hera.trace.etl.api.AttributeService;
import com.xiaomi.hera.trace.etl.domain.metrics.SpanHolder;
import com.xiaomi.mone.app.api.model.HeraMetaDataModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "service.selector.property", havingValue = "outer")
public class OzHeraMetaDataServiceImpl implements OzHeraMetaDataService {

    @Autowired
    private AttributeService attributeService;

    @Override
    public void syncHeraMetaData() {

    }

    @Override
    public HeraMetaDataModel getHeraMetaData(String peerIpPort) {
        return null;
    }

    @Override
    public HeraMetaDataModel getHeraMetaData(SpanHolder spanHolder) {
        return null;
    }

    @Override
    public String getMetricsMetaDataName(String peerIpPort) {
        if(StringUtils.isEmpty(peerIpPort)){
            return null;
        }
        HeraMetaDataModel heraMetaData = getHeraMetaData(peerIpPort);
        if(heraMetaData == null){
            return null;
        }
        String destApp = null;
        if(heraMetaData.getMetaId() == null){
            destApp = heraMetaData.getMetaName().replaceAll("-", "_");
        }else{
            destApp = heraMetaData.getMetaId() + "_" + heraMetaData.getMetaName().replaceAll("-", "_");
        }
        return destApp;
    }

    @Override
    public HeraMetaDataModel getMetaDataByDubbo(String dubboMeta) {
        return null;
    }

    @Override
    public void insert(HeraMetaDataModelDTO model) {

    }
}