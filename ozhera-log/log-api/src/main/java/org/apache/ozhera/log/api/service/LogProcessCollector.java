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
package org.apache.ozhera.log.api.service;

import org.apache.ozhera.log.api.model.meta.NodeCollInfo;
import org.apache.ozhera.log.api.model.vo.AgentLogProcessDTO;
import org.apache.ozhera.log.api.model.vo.TailLogProcessDTO;
import org.apache.ozhera.log.api.model.vo.UpdateLogProcessCmd;

import java.util.List;

/**
 * @author wtt
 * @version 1.0
 * @description
 * @date 2022/12/6 14:32
 */
public interface LogProcessCollector {

    /**
     * Update log collection progress
     *
     * @param cmd
     */
    void collectLogProcess(UpdateLogProcessCmd cmd);

    /**
     * Query the log collection progress of tail
     *
     * @param tailId
     * @param tailName
     * @param targetIp
     * @return
     */
    List<TailLogProcessDTO> getTailLogProcess(Long tailId, String tailName, String targetIp);

    /**
     * query by ip
     *
     * @param ip
     * @return
     */
    List<AgentLogProcessDTO> getAgentLogProcess(String ip);

    /**
     * The acquisition progress is less than the progress ratio
     *
     * @param progressRation Match the collection progress
     * @return
     */
    List<UpdateLogProcessCmd.CollectDetail> getColProcessImperfect(Double progressRation);

    /**
     * Get the collection details under a tail
     *
     * @param tailId
     * @return
     */
    List<UpdateLogProcessCmd.FileProgressDetail> getFileProcessDetailByTail(Long tailId);

    /**
     * get all coll detail
     *
     * @return
     */
    List<UpdateLogProcessCmd.CollectDetail> getAllCollectDetail(String ip);

    List<String> getAllAgentList();

    NodeCollInfo getNodeCollInfo(String ip);

}
