package com.alibabacloud.jenkins.ecs.util;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kunlun.ykl on 2020/11/20.
 */
public class NetworkUtilsTest {

    @Test
    public void autoGenerateSubnetTest() {
        List<String> otherVswCidrBlocks = Lists.newArrayList();
        //多个子网测试
        otherVswCidrBlocks.add("172.16.0.0/16");
//        otherVswCidrBlocks.add("172.16.1.0/24");
//        otherVswCidrBlocks.add("172.16.2.0/23");
        String s = NetworkUtils.autoGenerateSubnet("172.16.0.0/12", otherVswCidrBlocks);
        Assert.assertTrue(NetworkUtils.contains("172.16.0.0/12", s));
        Assert.assertFalse(NetworkUtils.parentOrSubNetwork("172.16.0.0/24", s));
        Assert.assertFalse(NetworkUtils.parentOrSubNetwork("172.16.2.0/23", s));
        //一个子网测试
        otherVswCidrBlocks = new ArrayList<>();
        otherVswCidrBlocks.add("192.168.0.0/26");
        s = NetworkUtils.autoGenerateSubnet("192.168.0.0/24", otherVswCidrBlocks);
        Assert.assertTrue(NetworkUtils.contains("192.168.0.0/24", s));
        Assert.assertFalse(NetworkUtils.parentOrSubNetwork(s,"192.168.0.0/26"));
        //父网测试
        otherVswCidrBlocks = new ArrayList<>();
        otherVswCidrBlocks.add("192.0.0.0/8");
        otherVswCidrBlocks.add("192.168.0.0/26");
        otherVswCidrBlocks.add("192.168.0.64/26");
        s = NetworkUtils.autoGenerateSubnet("192.168.0.0/24", otherVswCidrBlocks);
        Assert.assertTrue(NetworkUtils.contains("192.168.0.0/24", s));
        //不相关网络测试
        otherVswCidrBlocks = new ArrayList<>();
        otherVswCidrBlocks.add("172.16.0.128/16");
        s = NetworkUtils.autoGenerateSubnet("192.168.0.0/24", otherVswCidrBlocks);
        Assert.assertTrue(NetworkUtils.contains("192.168.0.0/24", s));
        Assert.assertFalse(NetworkUtils.contains("172.16.0.128/16", s));
        //空网络测试
        otherVswCidrBlocks = new ArrayList<>();
        s = NetworkUtils.autoGenerateSubnet("192.168.0.0/24", otherVswCidrBlocks);
        Assert.assertTrue(NetworkUtils.contains("192.168.0.0/24", s));
        //混合测试
        //不相关网络测试
        otherVswCidrBlocks = new ArrayList<>();
        otherVswCidrBlocks.add("172.16.0.128/16");
        otherVswCidrBlocks.add("192.0.0.0/4");
        otherVswCidrBlocks.add("172.16.0.0/24");
        otherVswCidrBlocks.add("172.16.1.0/23");
        otherVswCidrBlocks.add("192.168.0.0/26");
        s = NetworkUtils.autoGenerateSubnet("172.16.0.0/12", otherVswCidrBlocks);
        Assert.assertTrue(NetworkUtils.contains("172.16.0.0/12", s));
        Assert.assertFalse(NetworkUtils.contains("172.16.0.128/16", s));
        Assert.assertFalse(NetworkUtils.contains("172.16.1.0/23", s));
    }
}
