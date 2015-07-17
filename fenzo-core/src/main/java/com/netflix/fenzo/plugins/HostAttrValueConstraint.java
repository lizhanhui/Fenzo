/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.fenzo.plugins;

import com.netflix.fenzo.ConstraintEvaluator;
import com.netflix.fenzo.TaskRequest;
import com.netflix.fenzo.TaskTrackerState;
import com.netflix.fenzo.VirtualMachineCurrentState;
import com.netflix.fenzo.VirtualMachineLease;
import org.apache.mesos.Protos;
import com.netflix.fenzo.functions.Func1;

import java.util.Map;

/**
 * A constraint that ensures that a task gets a host with an attribute of a specified value.
 */
public class HostAttrValueConstraint implements ConstraintEvaluator {
    private static final String HOSTNAME="HOSTNAME";
    private final String hostAttributeName;
    private final Func1<String, String> hostAttributeValueGetter;

    public HostAttrValueConstraint(String hostAttributeName, Func1<String, String> hostAttributeValueGetter) {
        this.hostAttributeName = hostAttributeName==null? HOSTNAME:hostAttributeName;
        this.hostAttributeValueGetter = hostAttributeValueGetter;
    }

    /**
     * @warn method description missing
     *
     * @return
     */
    @Override
    public String getName() {
        return HostAttrValueConstraint.class.getName()+"-"+hostAttributeName;
    }

    /**
     * @warn method description missing
     * @warn parameter descriptions missing
     *
     * @param taskRequest
     * @param targetVM
     * @param taskTrackerState
     * @return
     */
    @Override
    public Result evaluate(TaskRequest taskRequest, VirtualMachineCurrentState targetVM, TaskTrackerState taskTrackerState) {
        String targetHostAttrVal = getAttrValue(targetVM.getCurrAvailableResources());
        if(targetHostAttrVal==null || targetHostAttrVal.isEmpty()) {
            return new Result(false, hostAttributeName + " attribute unavailable on host " + targetVM.getCurrAvailableResources().hostname());
        }
        String requiredAttrVal = hostAttributeValueGetter.call(taskRequest.getId());
        return targetHostAttrVal.equals(requiredAttrVal)?
                new Result(true, "") :
                new Result(false, "Host attribute " + hostAttributeName + ": required=" + requiredAttrVal + ", got=" + targetHostAttrVal);
    }

    private String getAttrValue(VirtualMachineLease lease) {
        switch (hostAttributeName) {
            case HOSTNAME:
                return lease.hostname();
            default:
                Map<String,Protos.Attribute> attributeMap = lease.getAttributeMap();
                if(attributeMap==null)
                    return null;
                Protos.Attribute attribute = attributeMap.get(hostAttributeName);
                if(attribute==null)
                    return null;
                return attribute.getText().getValue();
        }
    }
}