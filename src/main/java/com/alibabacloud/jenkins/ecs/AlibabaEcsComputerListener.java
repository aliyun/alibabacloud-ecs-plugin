package com.alibabacloud.jenkins.ecs;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;

/**
 * Created by kunlun.ykl on 2020/9/11.
 */
@Extension
public class AlibabaEcsComputerListener extends ComputerListener {
    @Override
    public void onOnline(Computer c, TaskListener listener) {
        if (c instanceof AlibabaEcsComputer) {
            ((AlibabaEcsComputer)c).onConnected();
        }
    }
}
