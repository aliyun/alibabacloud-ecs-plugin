package com.alibabacloud.jenkins.ecs.util;

import com.alibabacloud.jenkins.ecs.AlibabaCloud;
import hudson.model.Hudson.CloudList;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;

/**
 * Created by kunlun.ykl on 2020/8/27.
 */
public class CloudHelper {
    public static AlibabaCloud getCloud(String cloudName) {
        CloudList clouds = Jenkins.get().clouds;
        for (Cloud cloud : clouds) {
            if (!(cloud instanceof AlibabaCloud)) {
                continue;
            }
            if (cloud.getDisplayName().equals(cloudName)) {
                return (AlibabaCloud)cloud;
            }
        }
        return null;

    }

    public static String getTemplateId(String zone, String instanceType) {
        return zone + "-" + instanceType;
    }
}
