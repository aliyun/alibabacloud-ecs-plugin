package com.alibabacloud.jenkins.ecs;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Slave;
import hudson.slaves.SlaveComputer;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;

/**
 * Created by kunlun.ykl on 2020/8/25.
 */
@Slf4j
public class AlibabaEcsComputer extends SlaveComputer {

    public AlibabaEcsComputer(Slave follower) {
        super(follower);
    }

    public String getEcsType() {
        AlibabaEcsSpotFollower node = getNode();
        return node == null ? null : node.getInstanceType();
    }

    @CheckForNull
    public String getInstanceId() {
        AlibabaEcsSpotFollower node = getNode();
        return node == null ? null : node.getEcsInstanceId();
    }

    public AlibabaCloud getCloud() {
        AlibabaEcsSpotFollower node = getNode();
        return node == null ? null : node.getCloud();
    }

    @Override
    public AlibabaEcsSpotFollower getNode() {
        return (AlibabaEcsSpotFollower)super.getNode();
    }

    @Override
    public HttpResponse doDoDelete() {
        checkPermission(DELETE);
        log.info("doDoDelete node");
        AlibabaEcsSpotFollower node = getNode();
        if (node != null) {
            log.info("doDelete node: {}", node.getNodeName());
            node.terminate();
        }
        return new HttpRedirect("..");
    }

    public void onConnected() {
        AlibabaEcsSpotFollower node = getNode();
        if (node != null) {
            Boolean result =  node.onConnected();
            log.info("node.onConnected() result is:",result);
        }
    }

}
