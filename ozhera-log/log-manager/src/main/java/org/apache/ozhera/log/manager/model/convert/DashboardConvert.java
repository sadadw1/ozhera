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
package org.apache.ozhera.log.manager.model.convert;

import org.apache.ozhera.log.manager.model.dto.DashboardDTO;
import org.apache.ozhera.log.manager.model.dto.DashboardGraphDTO;
import org.apache.ozhera.log.manager.model.pojo.MilogAnalyseDashboardDO;
import org.apache.ozhera.log.manager.model.vo.CreateDashboardCmd;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface DashboardConvert {
    DashboardConvert INSTANCE = Mappers.getMapper(DashboardConvert.class);

    @Mappings({
            @Mapping(source = "dashboardDO.name", target = "dashboardName"),
            @Mapping(source = "dashboardDO.id", target = "dashboardId")
    })
    DashboardDTO fromDO(MilogAnalyseDashboardDO dashboardDO, List<DashboardGraphDTO> graphList);

    MilogAnalyseDashboardDO toDO(CreateDashboardCmd cmd);
}
