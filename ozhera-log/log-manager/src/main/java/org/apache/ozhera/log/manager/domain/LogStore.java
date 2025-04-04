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
package org.apache.ozhera.log.manager.domain;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.ozhera.log.api.model.vo.ResourceUserSimple;
import org.apache.ozhera.log.manager.mapper.MilogEsClusterMapper;
import org.apache.ozhera.log.manager.model.dto.EsInfoDTO;
import org.apache.ozhera.log.manager.model.pojo.MilogEsClusterDO;
import org.apache.ozhera.log.manager.model.pojo.MilogLogStoreDO;
import org.apache.ozhera.log.manager.model.pojo.MilogMiddlewareConfig;
import org.apache.ozhera.log.manager.model.vo.LogStoreParam;
import org.apache.ozhera.log.manager.service.impl.MilogMiddlewareConfigServiceImpl;
import com.xiaomi.youpin.docean.anno.Service;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
public class LogStore {
    @Resource
    private MilogMiddlewareConfigServiceImpl resourceConfigService;

    @Resource
    private MilogMiddlewareConfigServiceImpl milogMiddlewareConfigService;

    @Resource
    private MilogEsClusterMapper milogEsClusterMapper;

    @Resource
    private EsIndexTemplate esIndexTemplate;

    /**
     * ES resource binding
     */
    public void storeResourceBinding(MilogLogStoreDO ml, LogStoreParam cmd) {
        ResourceUserSimple resourceUserConfig = resourceConfigService.userResourceList(cmd.getMachineRoom(), cmd.getLogType());
        if (resourceUserConfig.getInitializedFlag()) {
            //Select the ES cluster
            if (null == cmd.getEsResourceId()) {
                List<MilogEsClusterDO> esClusterDOS = milogEsClusterMapper.selectList(Wrappers.lambdaQuery());
                cmd.setEsResourceId(esClusterDOS.get(esClusterDOS.size() - 1).getId());
            }
            EsInfoDTO esInfo = esIndexTemplate.getEsInfo(cmd.getEsResourceId(), cmd.getLogType(), null);
            cmd.setEsIndex(esInfo.getIndex());
            ml.setEsClusterId(esInfo.getClusterId());
            if (StringUtils.isEmpty(cmd.getEsIndex())) {
                ml.setEsIndex(esInfo.getIndex());
            } else {
                ml.setEsIndex(cmd.getEsIndex());
            }
            if (null == cmd.getMqResourceId()) {
                MilogMiddlewareConfig milogMiddlewareConfig = milogMiddlewareConfigService.queryMiddlewareConfigDefault(cmd.getMachineRoom());
                ml.setMqResourceId(milogMiddlewareConfig.getId());
                cmd.setMqResourceId(milogMiddlewareConfig.getId());
            }
        }
    }
}
