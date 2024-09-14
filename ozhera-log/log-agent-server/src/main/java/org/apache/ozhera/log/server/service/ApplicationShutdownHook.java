/*
 * Copyright (C) 2020 Xiaomi Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.ozhera.log.server.service;

import com.xiaomi.data.push.rpc.RpcServer;
import com.xiaomi.youpin.docean.anno.Component;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author wtt
 * @version 1.0
 * @description
 * @date 2023/10/13 14:11
 */
@Component
@Slf4j
public class ApplicationShutdownHook {

    @Resource
    private RpcServer rpcServer;

    public void init() {
        addRuntimeShutdownHook();
    }

    /**
     * addRuntimeShutdownHook server deregisterInstance
     */
    private void addRuntimeShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> rpcServer.shutdown()));
    }

}