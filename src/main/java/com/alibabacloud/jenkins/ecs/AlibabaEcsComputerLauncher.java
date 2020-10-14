package com.alibabacloud.jenkins.ecs;

import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

/**
 * Created by kunlun.ykl on 2020/8/24.
 */
public abstract class AlibabaEcsComputerLauncher extends ComputerLauncher {
    @Override
    public void launch(SlaveComputer slaveComputer, TaskListener listener) {
        launchScript((AlibabaEcsComputer)slaveComputer, listener);
    }

    /**
     * Stage 2 of the launch. Called after the ECS instance comes up.
     */
    protected abstract void launchScript(AlibabaEcsComputer computer, TaskListener listener);
}
