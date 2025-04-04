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
package org.apache.ozhera.log.manager.controller;

import org.apache.ozhera.log.common.Result;
import org.apache.ozhera.log.manager.model.StatisticsQuery;
import org.apache.ozhera.log.manager.model.dto.EsStatisticResult;
import org.apache.ozhera.log.manager.model.dto.EsStatisticsKeyWord;
import org.apache.ozhera.log.manager.model.vo.LogQuery;
import org.apache.ozhera.log.manager.service.StatisticsService;
import org.apache.ozhera.log.manager.service.impl.EsDataServiceImpl;
import com.xiaomi.youpin.docean.anno.Controller;
import com.xiaomi.youpin.docean.anno.RequestMapping;
import com.xiaomi.youpin.docean.anno.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class MilogStatisticController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MilogStatisticController.class);
    
    @Resource
    private StatisticsService statisticsService;
    
    @Resource
    private EsDataServiceImpl esDataService;
    
    @RequestMapping(path = "/milog/statistic/es")
    public Result<EsStatisticResult> statisticEs(@RequestParam("param") LogQuery param) throws Exception {
        return esDataService.EsStatistic(param);
    }
    
    @RequestMapping(path = "/log/queryTailStatisticsByHour")
    public Result<Map<String, Long>> queryTailStatisticsByHour(StatisticsQuery statisticsQuery) throws IOException {
        return statisticsService.queryTailStatisticsByHour(statisticsQuery);
    }
    
    @RequestMapping(path = "/log/queryStoreTopByDay")
    public Result<Map<String, Long>> queryStoreTopTailStatisticsByDay(StatisticsQuery statisticsQuery)
            throws IOException {
        return statisticsService.queryStoreTopTailStatisticsByDay(statisticsQuery);
    }
    
    @RequestMapping(path = "/log/querySpaceTopStore")
    public Result<Map<String, Long>> querySpaceTopStore(StatisticsQuery statisticsQuery) throws IOException {
        return statisticsService.querySpaceTopStoreByDay(statisticsQuery);
    }
    
    @RequestMapping(path = "/log/store/index/field/ration")
    public Result<List<EsStatisticsKeyWord>> queryEsStatisticsRation(LogQuery param) {
        return statisticsService.queryEsStatisticsRation(param);
    }
    
}
