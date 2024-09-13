/*
 *  Copyright (C) 2020 Xiaomi Corporation
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ozhera.monitor.service.alertmanager;

import org.apache.ozhera.monitor.dao.model.AppAlarmRule;
import org.apache.ozhera.monitor.dao.model.AppMonitor;
import org.apache.ozhera.monitor.service.model.prometheus.AlarmRuleData;

import java.util.Map;

/**
 * @author gaoxihui
 * @date 2023/10/6 11:38 上午
 */
public interface AlarmExprService {

    public String getExpr(AppAlarmRule rule, String scrapeIntervel, AlarmRuleData ruleData, AppMonitor app);

    public String getContainerCpuResourceAlarmExpr(Integer projectId,String projectName,String op,double value,boolean isK8s,AlarmRuleData ruleData);

    public String getContainerMemReourceAlarmExpr(Integer projectId,String projectName,String op,double value,boolean isK8s,AlarmRuleData ruleData);

    public Map getEnvIpMapping(Integer projectId, String projectName);
}