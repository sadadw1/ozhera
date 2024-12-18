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
package org.apache.ozhera.log.manager.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Pair;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import org.apache.ozhera.log.api.enums.LogStructureEnum;
import org.apache.ozhera.log.api.enums.LogTypeEnum;
import org.apache.ozhera.log.api.enums.OperateEnum;
import org.apache.ozhera.log.common.Result;
import org.apache.ozhera.log.exception.CommonError;
import org.apache.ozhera.log.manager.common.context.MoneUserContext;
import org.apache.ozhera.log.manager.common.validation.StoreValidation;
import org.apache.ozhera.log.manager.dao.MilogLogTailDao;
import org.apache.ozhera.log.manager.dao.MilogLogstoreDao;
import org.apache.ozhera.log.manager.domain.EsIndexTemplate;
import org.apache.ozhera.log.manager.domain.LogTail;
import org.apache.ozhera.log.manager.mapper.MilogEsIndexMapper;
import org.apache.ozhera.log.manager.model.convert.MilogLogstoreConvert;
import org.apache.ozhera.log.manager.model.dto.EsInfoDTO;
import org.apache.ozhera.log.manager.model.dto.LogStoreDTO;
import org.apache.ozhera.log.manager.model.dto.MapDTO;
import org.apache.ozhera.log.manager.model.dto.MenuDTO;
import org.apache.ozhera.log.manager.model.pojo.MilogEsIndexDO;
import org.apache.ozhera.log.manager.model.pojo.MilogLogStoreDO;
import org.apache.ozhera.log.manager.model.pojo.MilogLogTailDo;
import org.apache.ozhera.log.manager.model.pojo.MilogMiddlewareConfig;
import org.apache.ozhera.log.manager.model.vo.LogStoreParam;
import org.apache.ozhera.log.manager.service.BaseService;
import org.apache.ozhera.log.manager.service.LogStoreService;
import org.apache.ozhera.log.manager.service.extension.common.CommonExtensionService;
import org.apache.ozhera.log.manager.service.extension.common.CommonExtensionServiceFactory;
import org.apache.ozhera.log.manager.service.extension.resource.ResourceExtensionService;
import org.apache.ozhera.log.manager.service.extension.resource.ResourceExtensionServiceFactory;
import org.apache.ozhera.log.manager.service.extension.store.StoreExtensionService;
import org.apache.ozhera.log.manager.service.extension.store.StoreExtensionServiceFactory;
import com.xiaomi.youpin.docean.anno.Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ozhera.log.common.Constant.SUCCESS_MESSAGE;
import static org.apache.ozhera.log.manager.service.impl.EsDataServiceImpl.requiredFields;

/**
 * @author milog
 */
@Service
@Slf4j
public class LogStoreServiceImpl extends BaseService implements LogStoreService {

    @Resource
    private MilogLogstoreDao logStoreDao;

    @Resource
    private MilogLogTailDao milogLogtailDao;

    @Resource
    private EsIndexTemplate esIndexTemplate;

    @Resource
    private StoreValidation storeValidation;

    @Resource
    private MilogMiddlewareConfigServiceImpl milogMiddlewareConfigService;

    @Resource
    private MilogEsIndexMapper milogEsIndexMapper;

    @Resource
    private LogTail logTail;

    @Resource
    private LogTailServiceImpl logTailService;

    private StoreExtensionService storeExtensionService;

    private ResourceExtensionService resourceExtensionService;

    private CommonExtensionService commonExtensionService;

    /**
     * init method
     */
    public void init() {
        storeExtensionService = StoreExtensionServiceFactory.getStoreExtensionService();
        resourceExtensionService = ResourceExtensionServiceFactory.getResourceExtensionService();
        commonExtensionService = CommonExtensionServiceFactory.getCommonExtensionService();
    }

    @Override
    public Result<String> newLogStore(LogStoreParam cmd) {
        if (null != cmd.getId()) {
            return updateLogStore(cmd);
        } else {
            return createLogStore(cmd);
        }
    }

    private Result<String> createLogStore(LogStoreParam cmd) {
        String errorInfos = storeValidation.logStoreParamValid(cmd);
        if (StringUtils.isNotEmpty(errorInfos)) {
            return Result.failParam(errorInfos);
        }
        if (!cmd.getNameSameStatus() && logStoreDao.verifyExistByName(cmd.getLogstoreName(), null, cmd.getSpaceId())) {
            return new Result<>(CommonError.UnknownError.getCode(), "There is a store name with the same name", "");
        }
        List<MilogLogStoreDO> logStoreDOS = logStoreDao.queryBySpaceIdNamed(cmd.getSpaceId(), cmd.getLogstoreName());
        if (CollectionUtils.isNotEmpty(logStoreDOS)) {
            return Result.failParam("The store name is duplicated, please fill in the name again");
        }
        MilogLogStoreDO storeDO = MilogLogstoreConvert.INSTANCE.fromCommand(cmd);
        wrapBaseCommon(storeDO, OperateEnum.ADD_OPERATE);
        // Bind resources
        storeExtensionService.storeResourceBinding(storeDO, cmd, OperateEnum.ADD_OPERATE);
        checkRequiredFieldExist(storeDO, cmd);
        // storage
        boolean res = logStoreDao.newMilogLogStore(storeDO);
        if (res == true) {
            storeExtensionService.postProcessing(storeDO, cmd, OperateEnum.ADD_OPERATE);
            return new Result<>(CommonError.Success.getCode(), CommonError.Success.getMessage());
        } else {
            log.warn("[MilogLogstoreService.newMilogLogstore] creator MilogLogstore err,logstoreName:{}", cmd.getLogstoreName());
            return new Result<>(CommonError.UnknownError.getCode(), CommonError.UnknownError.getMessage());
        }

    }

    private void checkRequiredFieldExist(MilogLogStoreDO storeDO, LogStoreParam cmd) {
        String keyList = storeDO.getKeyList();
        String columnTypeList = storeDO.getColumnTypeList();
        for (Pair<String, Pair<String, Integer>> requiredField : requiredFields) {
            if (!keyList.contains(requiredField.getKey())) {
                keyList = String.format("%s,%s:%s", keyList, requiredField.getKey(), requiredField.getValue().getValue());
                columnTypeList = String.format("%s,%s", columnTypeList, requiredField.getValue().getKey());
            }
        }
        storeDO.setKeyList(keyList);
        storeDO.setColumnTypeList(columnTypeList);
        cmd.setKeyList(keyList);
        cmd.setColumnTypeList(columnTypeList);
    }

    @Override
    public MilogLogStoreDO buildLogStoreEsInfo(LogStoreParam storeParam, String creator) {
        // get esIndex
        EsInfoDTO esInfo = esIndexTemplate.getEsInfo(storeParam.getMachineRoom(), storeParam.getLogType());
        storeParam.setEsIndex(esInfo.getIndex());
        MilogLogStoreDO ml = MilogLogstoreConvert.INSTANCE.fromCommand(storeParam);
        ml.setEsClusterId(esInfo.getClusterId());
        wrapBaseCommon(ml, OperateEnum.ADD_OPERATE, creator);
        return ml;
    }

    @Override
    public Result<LogStoreDTO> getLogStoreById(Long id) {
        if (null == id) {
            return Result.failParam("id can not be empty");
        }
        MilogLogStoreDO milogLogStoreDO = logStoreDao.queryById(id);
        LogStoreDTO logStoreDTO = new LogStoreDTO();
        if (null != milogLogStoreDO) {
            BeanUtil.copyProperties(milogLogStoreDO, logStoreDTO);
            if (MoneUserContext.getCurrentUser().getIsAdmin()) {
                logStoreDTO.setSelectCustomIndex(Boolean.TRUE);
            }
            logStoreDTO.setEsResourceId(milogLogStoreDO.getEsClusterId());
            logStoreDTO.setLogTypeName(LogTypeEnum.queryNameByType(milogLogStoreDO.getLogType()));
            logStoreDTO.setMachineRoomName(commonExtensionService.getMachineRoomName(milogLogStoreDO.getMachineRoom()));
        }
        return Result.success(logStoreDTO);
    }

    public Result<List<MapDTO<String, Long>>> getLogStoreBySpaceId(Long spaceId) {
        List<MilogLogStoreDO> stores = logStoreDao.getMilogLogstoreBySpaceId(spaceId);
        ArrayList<MapDTO<String, Long>> ret = new ArrayList<>();
        for (MilogLogStoreDO s : stores) {
            ret.add(new MapDTO<>(s.getLogstoreName(), s.getId()));
        }
        return new Result<>(CommonError.Success.getCode(), CommonError.Success.getMessage(), ret);
    }

    public Result<Map<String, Object>> getLogStoreByPage(String logstoreName, Long spaceId, int page, int pagesize) {
        Map<String, Object> ret = logStoreDao.getMilogLogstoreByPage(logstoreName, spaceId, page, pagesize);
        return new Result<>(CommonError.Success.getCode(), CommonError.Success.getMessage(), ret);
    }

    public Result<Map<String, Object>> getAllLogStore() {
        Map<String, Object> ret = logStoreDao.getAllMilogLogstore(MoneUserContext.getCurrentUser().getZone());
        return new Result<>(CommonError.Success.getCode(), CommonError.Success.getMessage(), ret);
    }

    public Result<List<MilogLogStoreDO>> getLogStoreByIds(List<Long> ids) {
        List<MilogLogStoreDO> ret = logStoreDao.getMilogLogstore(ids);
        return new Result<>(CommonError.Success.getCode(), CommonError.Success.getMessage(), ret);
    }

    public Result<String> updateLogStore(LogStoreParam param) {
        MilogLogStoreDO milogLogstoreDO = logStoreDao.queryById(param.getId());
        if (null == milogLogstoreDO) {
            return new Result<>(CommonError.ParamsError.getCode(), "logstore not found ");
        }
        if (!param.getLogType().equals(milogLogstoreDO.getLogType()) && 0 != milogLogtailDao.getTailCount(param.getId())) {
            return new Result<>(CommonError.ParamsError.getCode(), "Logtail has been created under logstore and does not allow you to change the log type");
        }
        String errorInfos = storeValidation.logStoreParamValid(param);
        if (StringUtils.isNotEmpty(errorInfos)) {
            return Result.failParam(errorInfos);
        }
        if (logStoreDao.verifyExistByName(param.getLogstoreName(), param.getId(), param.getSpaceId())) {
            return new Result(CommonError.UnknownError.getCode(), "There is a store name with the same name", "");
        }

        MilogLogStoreDO ml = MilogLogstoreConvert.INSTANCE.fromCommand(param);
        ml.setEsClusterId(milogLogstoreDO.getEsClusterId());
        ml.setEsIndex(milogLogstoreDO.getEsIndex());
        ml.setCtime(milogLogstoreDO.getCtime());
        ml.setCreator(milogLogstoreDO.getCreator());
        // Select the corresponding index
        storeExtensionService.storeResourceBinding(ml, param, OperateEnum.UPDATE_OPERATE);
        wrapBaseCommon(ml, OperateEnum.UPDATE_OPERATE);
        checkRequiredFieldExist(ml, param);
        boolean updateRes = storeExtensionService.updateLogStore(ml);
        if (updateRes && storeExtensionService.sendConfigSwitch(param)) {
            storeExtensionService.postProcessing(ml, param, OperateEnum.UPDATE_OPERATE);
            //Check to see if there is a tail and if there is a resend configuration information (nacos and agent)
            logTail.handleStoreTail(milogLogstoreDO.getId());
            return new Result<>(CommonError.Success.getCode(), CommonError.Success.getMessage());
        }
        return Result.success(SUCCESS_MESSAGE);
    }

    public Result<Void> deleteLogStore(Long id) {
        MilogLogStoreDO logStore = logStoreDao.queryById(id);
        if (null == logStore) {
            return new Result<>(CommonError.ParamsError.getCode(), "logstore not found");
        }
        List<MilogLogTailDo> tails = milogLogtailDao.getMilogLogtailByStoreId(id);
        if (tails != null && tails.size() != 0) {
            return new Result<>(CommonError.ParamsError.getCode(), "There is a tail under the log store and cannot be deleted");
        }
        storeExtensionService.deleteStorePostProcessing(logStore);
        if (logStoreDao.deleteMilogSpace(id)) {
            storeExtensionService.postProcessing(logStore, null, OperateEnum.DELETE_OPERATE);
            //Delete the configuration in NACOS
            logTailService.deleteConfigRemote(logStore.getSpaceId(), id, logStore.getMachineRoom(), LogStructureEnum.STORE);
            return new Result<>(CommonError.Success.getCode(), CommonError.Success.getMessage());
        } else {
            log.warn("[MilogLogstoreService.deleteMilogLogstore] delete Milogstore err,spaceId:{}", id);
            return new Result<>(CommonError.UnknownError.getCode(), CommonError.UnknownError.getMessage());
        }
    }


    public Result<List<Map<String, String>>> getStoreIps(Long storeId) {
        if (storeId == null) {
            return Result.failParam("The parameter cannot be empty");
        }
        final List<MilogLogTailDo> logtailDoList = milogLogtailDao.getMilogLogtailByStoreId(storeId);
        List<Map<String, String>> res = new ArrayList<>();
        Map<String, String> ferry;
        Set<String> existIpSet = new HashSet<>();
        for (MilogLogTailDo tail : logtailDoList) {
            List<String> ipList = tail.getIps();
            if (CollectionUtils.isNotEmpty(ipList)) {
                for (String ip : ipList) {
                    if (existIpSet.contains(ip) || StringUtils.isEmpty(ip)) {
                        continue;
                    }
                    ferry = new HashMap<>(2);
                    ferry.put("label", ip);
                    ferry.put("value", ip);
                    res.add(ferry);
                    existIpSet.add(ip);
                }
            }
        }
        return Result.success(res);
    }

    public Result<List<MenuDTO<Long, String>>> queryDeptExIndexList(String regionCode, Integer logTypeCode) {
        if (StringUtils.isBlank(regionCode) || null == logTypeCode) {
            return Result.failParam("regionCode or logTypeCode can not empty");
        }
        //Query the ES information under the department to which the current user belongs
        List<MilogMiddlewareConfig> middlewareConfigEs = milogMiddlewareConfigService.getESConfigs(regionCode);
        middlewareConfigEs = resourceExtensionService.currentUserConfigFilter(middlewareConfigEs);
        List<MenuDTO<Long, String>> menuDTOS = middlewareConfigEs.stream().map(config -> {
            MenuDTO<Long, String> menuDTO = new MenuDTO<>();
            menuDTO.setKey(config.getId());
            menuDTO.setLabel(config.getAlias());
            menuDTO.setChildren(getExIndexByLogType(config.getId(), logTypeCode));
            return menuDTO;
        }).collect(Collectors.toList());
        return Result.success(menuDTOS);
    }

    private List<MenuDTO<Long, String>> getExIndexByLogType(Long clusterId, Integer logTypeCode) {
        QueryWrapper queryWrapper = new QueryWrapper<>().eq("cluster_id", clusterId).eq("log_type", logTypeCode);
        List<MilogEsIndexDO> esIndexDOS = milogEsIndexMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(esIndexDOS)) {
            return esIndexDOS.stream().map(indexDO -> {
                MenuDTO<Long, String> menuDTO = new MenuDTO<>();
                menuDTO.setKey(indexDO.getId());
                menuDTO.setLabel(indexDO.getIndexName());
                return menuDTO;
            }).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    public Result<String> redistributeStoreConfig(Long storeId) {
        if (null == storeId) {
            return Result.failParam("storeId can not be empty");
        }
        logTail.handleStoreTail(storeId);
        return Result.success(SUCCESS_MESSAGE);
    }
}
