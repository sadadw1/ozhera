package com.xiaomi.mone.monitor.service.aop.helper;

import com.xiaomi.mone.monitor.bo.AlertGroupInfo;
import com.xiaomi.mone.monitor.bo.AlertGroupParam;
import com.xiaomi.mone.monitor.bo.HeraReqInfo;
import com.xiaomi.mone.monitor.dao.HeraOperLogDao;
import com.xiaomi.mone.monitor.dao.model.HeraOperLog;
import com.xiaomi.mone.monitor.result.Result;
import com.xiaomi.mone.monitor.service.AlertGroupService;
import com.xiaomi.mone.monitor.service.aop.action.HeraRequestMappingAction;
import lombok.extern.slf4j.Slf4j;
import org.nutz.json.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @project: mimonitor
 * @author: zgf1
 * @date: 2022/1/13 16:01
 */
@Slf4j
@Service
public class HeraRequestMappingActionAlertGroupHelper {

    @Autowired
    private HeraOperLogDao heraOperLogDao;
    @Autowired
    private AlertGroupService alertGroupService;

    public Result<AlertGroupInfo> alertGroupDetailed(String user, Long id) {
        AlertGroupParam param = new AlertGroupParam();
        param.setId(id);
        return alertGroupService.alertGroupDetailed(user, param);
    }


    public void saveHeraOperLogs(Result<AlertGroupInfo> alertData, HeraOperLog operLog, HeraReqInfo heraReqInfo) {
        boolean beforeAction = operLog.getId() == null ? true : false;
        if (alertData != null) {
            if (beforeAction) {
                operLog.setBeforeData(Json.toJson(alertData));
            } else {
                operLog.setAfterData(Json.toJson(alertData));
            }
        }
        operLog.setDataType(HeraRequestMappingAction.DATA_ALERT_GROUP);
        operLog.setLogType(HeraRequestMappingAction.LOG_TYPE_PARENT);
        if(!heraOperLogDao.insertOrUpdate(operLog)) {
            log.error("operate log AOP intercept insert or update exception; heraReqInfo={},operLog={}", heraReqInfo, operLog);
            return;
        }
    }
}
