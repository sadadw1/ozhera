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
package org.apache.ozhera.log.agent.filter;

import org.apache.ozhera.log.agent.filter.ratelimit.RatelimitFilter;
import org.apache.ozhera.log.api.model.meta.FilterConf;
import org.apache.ozhera.log.api.model.meta.FilterType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Filter chain implementation class
 */
@Slf4j
public class FilterChain {
    private CopyOnWriteArrayList<MilogFilter> filterList = new CopyOnWriteArrayList<>();

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile Invoker invoker;

    public void reset() {
        lock.writeLock().lock();
        try {
            Invoker curr = () -> {
            };
            int size = filterList.size();
            // The element that comes before is executed later, i.e. the larger the order, the later it is executed
            for (int i = 0; i < size; i++) {
                MilogFilter filter = filterList.get(i);
                Invoker last = curr;
                curr = () -> filter.doFilter(last);
            }
            this.invoker = curr;
        } catch (Exception e) {
            log.error("Filter Chain reload err", e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    public void loadFilterList(List<FilterConf> confs) {
        if (confs == null || confs.size() < 1) {
            return;
        }
        List<FilterConf> sortedConf = confs.stream().filter(Objects::nonNull).sorted((a, b) -> {
            Integer x = a.getOrder();
            Integer y = b.getOrder();
            // The order value is large first
            return y.compareTo(x);
        }).sorted((a, b) -> {
            // The filter for the global attribute comes last
            if (FilterType.GLOBAL.equals(a.getType()) && FilterType.GLOBAL.equals(b.getType())) {
                return 0;
            } else if (FilterType.GLOBAL.equals(a.getType())) {
                return 1;
            } else if (FilterType.GLOBAL.equals(b.getType())) {
                return -1;
            } else {
                return 0;
            }
        }).toList();
        CopyOnWriteArrayList<MilogFilter> filters = new CopyOnWriteArrayList<>();
        for (FilterConf conf : sortedConf) {
            switch (conf.getName()) {
                case RATELIMITER:
                    RatelimitFilter ratelimitFilter = new RatelimitFilter();
                    if (ratelimitFilter.init(conf)) {
                        filters.add(ratelimitFilter);
                    }
                    break;
                default:
                    break;
            }
        }
        this.filterList = filters;
    }

    public void doFilter() {
        try {
            lock.readLock().lock();
            this.invoker.doInvoker();
        } finally {
            lock.readLock().unlock();
        }
    }
}
