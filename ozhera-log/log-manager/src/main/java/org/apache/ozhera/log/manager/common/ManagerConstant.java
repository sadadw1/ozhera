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
package org.apache.ozhera.log.manager.common;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * @author wtt
 * @version 1.0
 * @description
 * @date 2022/5/11 10:42
 */
public class ManagerConstant {

    public static final String TPC_HOME_URL_HEAD = "tpc_home_url_head";

    public static final String SPACE_PAGE_URL = "/milog/space/getbypage";

    public static final String DEPT_LEVEL_PREFIX = "Department level";

    public static final String DEPT_NAME_PREFIX = "Department name";

    public static final List<String> RESOURCE_DEFAULT_INITIALIZED_DEPT = Lists.newArrayList("open source");

    public static final String RESOURCE_NOT_INITIALIZED_MESSAGE = ",Go to the Resource Management page to initialize resources";

    public static final Integer USER_DEPT_MAX_DEFAULT_LEVEL = 1;

    public static final String ES_LABEL = "open source";
}
