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
package org.apache.ozhera.log.manager.filter.cons;

import org.apache.ozhera.log.api.enums.RateLimitEnum;
import org.apache.ozhera.log.api.filter.Common;
import org.apache.ozhera.log.api.model.meta.FilterDefine;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashMap;

public class RateLimitConsTest extends TestCase {

    public void testConsRateimitFilterDefine() {
        FilterDefine filterDefine = FilterDefine.consRateLimitFilterDefine("FAST");
        System.out.println(filterDefine);
    }

    public void testConsTailRate() {
        FilterDefine d1 = new FilterDefine();
        FilterDefine d2 = new FilterDefine();
        d1.setCode(Common.RATE_LIMIT_CODE + 3);
        d1.setArgs(new HashMap<String, String>() {{
            put(Common.PERMITS_PER_SECOND, "100");
        }});
        String s = RateLimitEnum.consTailRate(Arrays.asList(d2, d1));
        System.out.println(s);
    }
}