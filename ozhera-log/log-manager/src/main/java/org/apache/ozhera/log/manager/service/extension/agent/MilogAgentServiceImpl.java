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
package org.apache.ozhera.log.manager.service.extension.agent;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.xiaomi.data.push.context.AgentContext;
import com.xiaomi.data.push.rpc.netty.AgentChannel;
import org.apache.ozhera.app.api.response.AppBaseInfo;
import org.apache.ozhera.log.api.enums.LogTypeEnum;
import org.apache.ozhera.log.api.enums.MQSourceEnum;
import org.apache.ozhera.log.api.enums.OperateEnum;
import org.apache.ozhera.log.api.model.meta.*;
import org.apache.ozhera.log.api.model.vo.AgentLogProcessDTO;
import org.apache.ozhera.log.api.service.PublishConfigService;
import org.apache.ozhera.log.common.Constant;
import org.apache.ozhera.log.common.Result;
import org.apache.ozhera.log.manager.common.Utils;
import org.apache.ozhera.log.manager.dao.MilogAppMiddlewareRelDao;
import org.apache.ozhera.log.manager.dao.MilogLogTailDao;
import org.apache.ozhera.log.manager.dao.MilogLogstoreDao;
import org.apache.ozhera.log.manager.dao.MilogMiddlewareConfigDao;
import org.apache.ozhera.log.manager.domain.LogProcess;
import org.apache.ozhera.log.manager.model.bo.MilogAgentIpParam;
import org.apache.ozhera.log.manager.model.pojo.MilogAppMiddlewareRel;
import org.apache.ozhera.log.manager.model.pojo.MilogLogStoreDO;
import org.apache.ozhera.log.manager.model.pojo.MilogLogTailDo;
import org.apache.ozhera.log.manager.model.pojo.MilogMiddlewareConfig;
import org.apache.ozhera.log.manager.service.env.HeraEnvIpService;
import org.apache.ozhera.log.manager.service.env.HeraEnvIpServiceFactory;
import org.apache.ozhera.log.manager.service.impl.HeraAppServiceImpl;
import org.apache.ozhera.log.manager.service.impl.LogTailServiceImpl;
import org.apache.ozhera.log.manager.service.path.LogPathMapping;
import org.apache.ozhera.log.manager.service.path.LogPathMappingFactory;
import com.xiaomi.youpin.docean.anno.Service;
import com.xiaomi.youpin.docean.common.NamedThreadFactory;
import com.xiaomi.youpin.docean.plugin.dubbo.anno.Reference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.ozhera.log.common.Constant.SYMBOL_COLON;
import static org.apache.ozhera.log.manager.service.extension.agent.MilogAgentService.DEFAULT_AGENT_EXTENSION_SERVICE_KEY;

@Service(name = DEFAULT_AGENT_EXTENSION_SERVICE_KEY)
@Slf4j
public class MilogAgentServiceImpl implements MilogAgentService {

    @Resource
    private LogPathMappingFactory logPathMappingFactory;

    @Resource
    private HeraEnvIpServiceFactory heraEnvIpServiceFactory;

    @Resource
    private MilogLogTailDao milogLogtailDao;

    @Resource
    private MilogLogstoreDao logstoreDao;

    private Gson gson = Constant.GSON;

    @Resource
    private LogProcess logProcess;

    @Resource
    private MilogAppMiddlewareRelDao milogAppMiddlewareRelDao;
    @Resource
    private MilogMiddlewareConfigDao milogMiddlewareConfigDao;

    @Resource
    private HeraAppServiceImpl heraAppService;
    @Resource
    private LogTailServiceImpl logTailService;

    @Reference(interfaceClass = PublishConfigService.class, group = "$dubbo.env.group", check = false, timeout = 14000)
    private PublishConfigService publishConfigService;

    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR;


    private static final AtomicInteger COUNT_INCR = new AtomicInteger(0);

    static {
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(6, 20,
                1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(200),
                new NamedThreadFactory("coll-base-data-start", true),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        THREAD_POOL_EXECUTOR.allowCoreThreadTimeOut(true);
    }

    @Override
    public Result<List<AgentLogProcessDTO>> process(String ip) {
        List<AgentLogProcessDTO> dtoList = logProcess.getAgentLogProcess(ip);
        return Result.success(dtoList);
    }

    @Override
    public Result<String> configIssueAgent(String agentId, String agentIp, String agentMachine) {
        if (StringUtils.isEmpty(agentIp)) {
            return Result.failParam("The agent IP cannot be empty");
        }
        // 1.Query the full configuration of the physical machine
        LogCollectMeta logCollectMeta = queryMilogAgentConfig(agentId, agentIp, agentMachine);
        log.info("{},this ip config data:{}", agentIp, gson.toJson(logCollectMeta));
        String k8sNodeIP = queryNodeIpByPodIp(agentIp);
        if (StringUtils.isNotEmpty(k8sNodeIP)) {
            log.info("query k8s ip succeed,ip:{},k8sNodeIP:{}", agentIp, k8sNodeIP);
            agentIp = k8sNodeIP;
        }
        List<String> ipAddress = Lists.newArrayList();
        AgentContext.ins().map.entrySet().forEach(agentChannelEntry -> {
                    String key = agentChannelEntry.getKey();
                    ipAddress.add(StringUtils.substringBefore(key, SYMBOL_COLON));
                }
        );
        log.info("agent ip list:{}", gson.toJson(ipAddress));
        //2.Delivery configuration
        sengConfigToAgent(agentIp, logCollectMeta);
        return Result.success("success");
    }

    /**
     * Deliver the configuration to the specified IP address
     *
     * @param logCollectMeta
     * @param agentIp
     */
    public void sengConfigToAgent(final String agentIp, LogCollectMeta logCollectMeta) {
        if (CollectionUtils.isEmpty(logCollectMeta.getAppLogMetaList()) || logCollectMeta.getAppLogMetaList()
                .stream().allMatch(appLogMeta -> CollectionUtils.isEmpty(appLogMeta.getLogPatternList()))) {
            return;
        }
        // Placed in the thread pool for execution
        THREAD_POOL_EXECUTOR.execute(() -> {
            publishConfigService.sengConfigToAgent(agentIp, logCollectMeta);
        });
    }

    /**
     * Send the configuration of the delta
     * 1.Find all the physical IP addresses where the app is deployed
     * 2.This new configuration is pushed to these physical machines or containers
     *
     * @param milogAppId
     * @param ips
     */
    @Override
    public void publishIncrementConfig(Long tailId, Long milogAppId, List<String> ips) {
        log.info("push agent params,milogAppId:{},ips:{}", milogAppId, ips);
        if (CollectionUtils.isEmpty(ips)) {
            return;
        }
        printMangerInfo();
        AppBaseInfo appBaseInfo = heraAppService.queryById(milogAppId);
        ips.forEach(ip -> {
            AppLogMeta appLogMeta = assembleSingleConfig(milogAppId, queryLogPattern(milogAppId, ip, appBaseInfo.getPlatformType()));
            LogCollectMeta logCollectMeta = new LogCollectMeta();
            logCollectMeta.setAgentIp(ip);
            logCollectMeta.setAppLogMetaList(Arrays.asList(appLogMeta));
            AgentDefine agentDefine = new AgentDefine();
            agentDefine.setFilters(new ArrayList<>());
            logCollectMeta.setAgentDefine(agentDefine);
            log.info("push agent config data,ip:{},{}", ip, gson.toJson(logCollectMeta));
            sengConfigToAgent(ip, logCollectMeta);
        });
    }

    @NotNull
    private Map<String, AgentChannel> getAgentChannelMap() {
        Map<String, AgentChannel> logAgentMap = new HashMap<>();
        AgentContext.ins().map.forEach((k, v) -> logAgentMap.put(StringUtils.substringBefore(k, SYMBOL_COLON), v));
        return logAgentMap;
    }

    private void printMangerInfo() {
        List<String> remoteAddress = publishConfigService.getAllAgentList();
        if (COUNT_INCR.getAndIncrement() % 200 == 0) {
            log.info("The set of remote addresses for the connected agent machine is:{}", gson.toJson(remoteAddress));
        }
    }

    public String queryNodeIpByPodIp(String ip) {
        return ip;
    }


    @Override
    public void publishIncrementDel(Long tailId, Long milogAppId, List<String> ips) {
        log.info("Delete the configuration synchronization to logAgent,tailId:{},milogAppId:{},ips:{}", tailId, milogAppId, gson.toJson(ips));
        AppLogMeta appLogMeta = new AppLogMeta();
        LogPattern logPattern = new LogPattern();
        assemblyAppInfo(milogAppId, appLogMeta);
        logPattern.setLogtailId(tailId);
        logPattern.setOperateEnum(OperateEnum.DELETE_OPERATE);

        appLogMeta.setLogPatternList(Arrays.asList(logPattern));
        ips.forEach(ip -> {
            LogCollectMeta logCollectMeta = new LogCollectMeta();
            logCollectMeta.setAgentIp(ip);
            logCollectMeta.setAgentMachine("");
            logCollectMeta.setAgentId("");
            logCollectMeta.setAppLogMetaList(Arrays.asList(appLogMeta));
            sengConfigToAgent(ip, logCollectMeta);
        });
    }

    private void assemblyAppInfo(Long milogAppId, AppLogMeta appLogMeta) {
        AppBaseInfo appBaseInfo = heraAppService.queryById(milogAppId);
        appLogMeta.setAppId(milogAppId);
        if (null != appBaseInfo) {
            appLogMeta.setAppName(appBaseInfo.getAppName());
        }
    }

    @Override
    public void delLogCollDirectoryByIp(Long tailId, String directory, List<String> ips) {
        log.info("delLogCollDirectoryByIp logAgent,tailId:{},directory:{},ips:{}", tailId, directory, gson.toJson(ips));
        AppLogMeta appLogMeta = new AppLogMeta();
        LogPattern logPattern = new LogPattern();
        logPattern.setLogtailId(tailId);
        logPattern.setOperateEnum(OperateEnum.DELETE_OPERATE);
        appLogMeta.setLogPatternList(Arrays.asList(logPattern));
        LogCollectMeta logCollectMeta = new LogCollectMeta();
        for (String ip : ips) {
            logCollectMeta.setAgentIp(ip);
            logCollectMeta.setDelDirectory(directory);
            logCollectMeta.setAppLogMetaList(Arrays.asList(appLogMeta));
            sengConfigToAgent(ip, logCollectMeta);
        }
    }

    @Override
    public Result<String> agentOfflineBatch(MilogAgentIpParam agentIpParam) {
        if (null == agentIpParam || CollectionUtils.isEmpty(agentIpParam.getIps())) {
            return Result.failParam("IP cannot be empty");
        }
        return null;
    }

    @Override
    public LogCollectMeta getLogCollectMetaFromManager(String ip) {
        return queryMilogAgentConfig("", ip, "");
    }

    /**
     * Query the full configuration of the IP address of the physical machine
     *
     * @param agentId
     * @param agentIp      Physical IP
     * @param agentMachine
     * @return
     */
    public LogCollectMeta queryMilogAgentConfig(String agentId, String agentIp, String agentMachine) {
        LogCollectMeta logCollectMeta = buildLogCollectMeta(agentIp);
        List<AppBaseInfo> appBaseInfos = Lists.newArrayList();
        List<MilogLogTailDo> logTailDos = milogLogtailDao.queryByIp(agentIp);
        if (CollectionUtils.isNotEmpty(logTailDos)) {
            appBaseInfos = heraAppService.queryByIds(
                    logTailDos.stream().map(MilogLogTailDo::getMilogAppId)
                            .distinct()
                            .collect(Collectors.toList())
            );
        }
        logCollectMeta.setAppLogMetaList(appBaseInfos.stream()
                .map(appBaseInfo -> assembleSingleConfig(appBaseInfo.getId().longValue(), queryLogPattern(appBaseInfo.getId().longValue(), agentIp, appBaseInfo.getPlatformType())))
                .filter(appLogMeta -> CollectionUtils.isNotEmpty(appLogMeta.getLogPatternList()))
                .collect(Collectors.toList()));
        return logCollectMeta;
    }

    private LogCollectMeta buildLogCollectMeta(String agentIp) {
        LogCollectMeta logCollectMeta = new LogCollectMeta();
        logCollectMeta.setAgentIp(agentIp);
        logCollectMeta.setAgentMachine("");
        logCollectMeta.setAgentId("");
        return logCollectMeta;
    }

    /**
     * Assemble incremental configurations
     *
     * @param milogAppId
     * @param logPatternList
     * @return
     */
    private AppLogMeta assembleSingleConfig(Long milogAppId, List<LogPattern> logPatternList) {
        AppLogMeta appLogMeta = new AppLogMeta();
        assemblyAppInfo(milogAppId, appLogMeta);
        appLogMeta.setLogPatternList(logPatternList);
        return appLogMeta;
    }

    private MQConfig decorateMQConfig(MilogLogTailDo milogLogtailDo) {
        MQConfig mqConfig = new MQConfig();
        try {
            Long mqResourceId = logstoreDao.queryById(milogLogtailDo.getStoreId()).getMqResourceId();

            List<MilogAppMiddlewareRel> milogAppMiddlewareRels = milogAppMiddlewareRelDao.queryByCondition(milogLogtailDo.getMilogAppId(), mqResourceId, milogLogtailDo.getId());

            if (CollectionUtils.isEmpty(milogAppMiddlewareRels)) {
                milogAppMiddlewareRels = milogAppMiddlewareRelDao.queryByCondition(milogLogtailDo.getMilogAppId(), null, milogLogtailDo.getId());
            }

            MilogAppMiddlewareRel milogAppMiddlewareRel = milogAppMiddlewareRels.get(milogAppMiddlewareRels.size() - 1);

            MilogMiddlewareConfig middlewareConfig = milogMiddlewareConfigDao.queryById(milogAppMiddlewareRel.getMiddlewareId());
            mqConfig.setClusterInfo(middlewareConfig.getNameServer());
            fillMqConfigData(mqConfig, MQSourceEnum.queryName(middlewareConfig.getType()), middlewareConfig, milogAppMiddlewareRel.getConfig());
        } catch (Exception e) {
            log.error("The assembly MQ configuration information is abnormal,data:{}", gson.toJson(milogLogtailDo), e);
        }
        return mqConfig;
    }

    private void fillMqConfigData(MQConfig mqConfig, String typeName, MilogMiddlewareConfig middlewareConfig, MilogAppMiddlewareRel.Config config) {
        mqConfig.setType(typeName);
        mqConfig.setAk(middlewareConfig.getAk());
        mqConfig.setProducerGroup(config.getConsumerGroup());
        mqConfig.setSk(middlewareConfig.getSk());
        mqConfig.setTopic(config.getTopic());
        mqConfig.setTag(config.getTag());
        mqConfig.setPartitionCnt(config.getPartitionCnt());
        mqConfig.setEsConsumerGroup(config.getEsConsumerGroup());
        mqConfig.setBatchSendSize(config.getBatchSendSize());
    }

    private List<LogPattern> queryLogPattern(Long milogAppId, String agentIp, Integer type) {
        List<MilogLogTailDo> milogLogtailDos = milogLogtailDao.queryByAppIdAgentIp(milogAppId, agentIp);
        if (CollectionUtils.isNotEmpty(milogLogtailDos)) {
            return milogLogtailDos.stream().map(milogLogtailDo -> {
                log.info("assemble data:{}", gson.toJson(milogAppId));
                LogPattern logPattern = generateLogPattern(milogLogtailDo);
                logPattern.setIps(Lists.newArrayList(agentIp));
                logPattern.setIpDirectoryRel(Lists.newArrayList(LogPattern.IPRel.builder().ip(agentIp).build()));
                LogPathMapping logPathMapping = logPathMappingFactory.queryLogPathMappingByAppType(type);
                HeraEnvIpService heraEnvIpService = heraEnvIpServiceFactory.getHeraEnvIpServiceByAppType(type);
                try {
                    logPattern.setLogPattern(logPathMapping.getLogPath(milogLogtailDo.getLogPath(), null));
                    logPattern.setLogSplitExpress(logPathMapping.getLogPath(milogLogtailDo.getLogSplitExpress(), null));
                    logPattern.setIpDirectoryRel(heraEnvIpService.queryActualIps(milogLogtailDo.getIps(), agentIp, milogLogtailDo.getLogPath()));
                } catch (Exception e) {
                    log.error("assemble log path data error:", e);
                }
                //Set the MQ configuration
                MQConfig mqConfig = decorateMQConfig(milogLogtailDo);
                logPattern.setMQConfig(mqConfig);
                return logPattern;
            }).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    private LogPattern generateLogPattern(MilogLogTailDo milogLogtailDo) {
        LogPattern logPattern = new LogPattern();
        MilogLogStoreDO milogLogstoreDO = logstoreDao.queryById(milogLogtailDo.getStoreId());
        logPattern.setLogtailId(milogLogtailDo.getId());
        logPattern.setTailName(milogLogtailDo.getTail());
        logPattern.setLogPattern(milogLogtailDo.getLogPath());
        logPattern.setLogSplitExpress(milogLogtailDo.getLogSplitExpress());
        logPattern.setFilters(milogLogtailDo.getFilter());
        logPattern.setFirstLineReg(milogLogtailDo.getFirstLineReg());
        if (null != milogLogstoreDO && null != milogLogstoreDO.getLogType()) {
            logPattern.setLogType(milogLogstoreDO.getLogType());
            if (LogTypeEnum.NGINX.getType().equals(milogLogstoreDO.getLogType())) {
                logPattern.setLogPattern(milogLogtailDo.getLogPath());
            }
        }
        if (null != milogLogtailDo.getFilterLogLevelList() && !milogLogtailDo.getFilterLogLevelList().isEmpty()) {
            logPattern.setFilterLogLevelList(milogLogtailDo.getFilterLogLevelList());
        }
        String tag = Utils.createTag(milogLogtailDo.getSpaceId(), milogLogtailDo.getStoreId(), milogLogtailDo.getId());
        logPattern.setPatternCode(tag);
        return logPattern;
    }
}
