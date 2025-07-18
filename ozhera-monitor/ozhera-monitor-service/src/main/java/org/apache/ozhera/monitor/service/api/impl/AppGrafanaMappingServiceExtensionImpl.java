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
package org.apache.ozhera.monitor.service.api.impl;

import org.apache.ozhera.monitor.dao.model.GrafanaTemplate;
import org.apache.ozhera.monitor.service.api.AppGrafanaMappingServiceExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @Description
 * @Date 2023/4/21 11:32 AM
 */
@Service
@ConditionalOnProperty(name = "service.selector.property", havingValue = "outer")
public class AppGrafanaMappingServiceExtensionImpl implements AppGrafanaMappingServiceExtension {
    @Override
    public void setPlatFormByLanguage(GrafanaTemplate template, String appLanguage) {

    }

    @Override
    public void dealRequestGrafanaTemplateCode(Integer code, String bindId, String appName) {

    }
}
