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
package org.apache.ozhera.log.manager.service.init_sql;

import org.apache.ozhera.log.api.enums.MachineRegionEnum;
import org.apache.ozhera.log.api.enums.MiddlewareEnum;
import org.apache.ozhera.log.api.enums.OperateEnum;
import org.apache.ozhera.log.common.Constant;
import org.apache.ozhera.log.manager.dao.MilogMiddlewareConfigDao;
import org.apache.ozhera.log.manager.model.pojo.MilogMiddlewareConfig;
import org.apache.ozhera.log.manager.service.BaseService;
import com.xiaomi.youpin.docean.anno.Service;
import com.xiaomi.youpin.docean.plugin.config.anno.Value;

import javax.annotation.Resource;
import java.util.Objects;

import static org.apache.ozhera.log.common.Constant.DEFAULT_OPERATOR;

/**
 * @author wtt
 * @version 1.0
 * @description Initialize the default NACOS configuration information into the table
 * @date 2023/3/3 10:45
 */
@Service
public class NcosConfigSqlService extends BaseService {
    private static final String DEFAULT_NCOS_ALIAS = "system nacos";

    @Resource
    private MilogMiddlewareConfigDao milogMiddlewareConfigDao;

    @Value(value = "$defaultNacosAddres")
    private String defaultNacosAddress;

    public void init() {
        String defaultRegionCode = MachineRegionEnum.CN_MACHINE.getEn();
        MilogMiddlewareConfig middlewareConfig = milogMiddlewareConfigDao.queryCurrentEnvNacos(defaultRegionCode);
        if (null == middlewareConfig) {
            addNcosConfig(defaultRegionCode);
            return;
        }
        if (Objects.equals(middlewareConfig.getNameServer(), defaultNacosAddress)) {
            updateNcosAddress(middlewareConfig);
        }
    }

    private void addNcosConfig(String defaultRegionCode) {
        MilogMiddlewareConfig middlewareConfig;
        middlewareConfig = new MilogMiddlewareConfig();
        middlewareConfig.setType(MiddlewareEnum.NCOS.getCode());
        middlewareConfig.setRegionEn(defaultRegionCode);
        middlewareConfig.setAlias(DEFAULT_NCOS_ALIAS);
        middlewareConfig.setNameServer(defaultNacosAddress);
        middlewareConfig.setIsDefault(Constant.YES.intValue());
        wrapBaseCommon(middlewareConfig, OperateEnum.ADD_OPERATE, DEFAULT_OPERATOR);
        milogMiddlewareConfigDao.addMiddlewareConfig(middlewareConfig);
    }

    private void updateNcosAddress(MilogMiddlewareConfig middlewareConfig) {
        middlewareConfig.setNameServer(defaultNacosAddress);
        wrapBaseCommon(middlewareConfig, OperateEnum.UPDATE_OPERATE, DEFAULT_OPERATOR);
        milogMiddlewareConfigDao.updateMiddlewareConfig(middlewareConfig);
    }

}
