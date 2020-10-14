package com.alibabacloud.jenkins.ecs.monitor;

import java.io.IOException;

import hudson.Extension;
import hudson.Functions;
import hudson.Util;
import hudson.model.Computer;
import hudson.node_monitors.AbstractAsyncNodeMonitorDescriptor;
import hudson.node_monitors.AbstractNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import hudson.node_monitors.SwapSpaceMonitor;
import jenkins.security.MasterToSlaveCallable;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.jvnet.hudson.MemoryMonitor;
import org.jvnet.hudson.MemoryUsage;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Created by kunlun.ykl on 2020/9/14.
 */
public class FreeMemMonitor extends SwapSpaceMonitor {
    /**
     * Returns the HTML representation of the space.
     */
    public String toHtml(MemoryUsage usage) {
        if (usage.availablePhysicalMemory == -1) { return "N/A"; }

        String humanReadableSpace = Functions.humanReadableByteSize(usage.availablePhysicalMemory);

        long free = usage.availablePhysicalMemory;
        free /= 1024L;   // convert to KB
        free /= 1024L;   // convert to MB
        if (free > 256 || usage.totalPhysicalMemory < usage.availablePhysicalMemory * 2) {
            return humanReadableSpace; // if we have more than 256MB free or less than 80% filled up, it's OK
        }

        // Otherwise considered dangerously low.
        return Util.wrapToErrorSpan(humanReadableSpace);
    }

    public long toMB(MemoryUsage usage) {
        if (usage.availablePhysicalMemory == -1) { return -1; }

        long free = usage.availablePhysicalMemory;
        free /= 1024L;   // convert to KB
        free /= 1024L;   // convert to MB
        return free;
    }

    @Override
    public String getColumnCaption() {
        // Hide this column from non-admins
        return "Free Mem Space";
    }

    @Extension
    @Symbol("memorySpace")
    public static class DescriptorImpl extends AbstractAsyncNodeMonitorDescriptor<MemoryUsage> {
        public DescriptorImpl() {
            setDescriptor(this);
        }

        private static void setDescriptor(AbstractNodeMonitorDescriptor<MemoryUsage> descriptor){
            DESCRIPTOR = descriptor;
        }

        @Override
        protected MonitorTask createCallable(Computer c) {
            return new MonitorTask();
        }

        public String getDisplayName() {
            return "Free Mem Space";
        }

        @Override
        public NodeMonitor newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new FreeMemMonitor();
        }
    }

    private static class MonitorTask extends MasterToSlaveCallable<MemoryUsage, IOException> {
        public MemoryUsage call() throws IOException {
            MemoryMonitor mm;
            try {
                mm = MemoryMonitor.get();
            } catch (IOException e) {
                return report(e);
            } catch (LinkageError e) { // JENKINS-15796
                return report(e);
            }
            return new MemoryUsage2(mm.monitor());
        }

        private <T extends Throwable> MemoryUsage report(T e) throws T {
            if (!warned) {
                warned = true;
                throw e;
            } else { // JENKINS-2194
                return null;
            }
        }

        private static final long serialVersionUID = 1L;

        private static boolean warned = false;
    }

}
