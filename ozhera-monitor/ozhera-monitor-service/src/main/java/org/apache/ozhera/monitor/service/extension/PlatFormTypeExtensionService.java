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

package org.apache.ozhera.monitor.service.extension;

import org.apache.ozhera.monitor.bo.Pair;
import org.apache.ozhera.monitor.bo.PlatForm;

import java.util.List;

/**
 * @author shanwb
 * @date 2023-04-20
 */
public interface PlatFormTypeExtensionService {

    boolean belongPlatForm(Integer typeCode, PlatForm platForm);

    Integer getMarketType(Integer typeCode);

    List<Pair>  getPlatFormTypeDescList();

    String getGrafanaDirByTypeCode(Integer typeCode);

    boolean checkTypeCode(Integer typeCode);

    Integer getTypeCodeByName(String typeName);

}