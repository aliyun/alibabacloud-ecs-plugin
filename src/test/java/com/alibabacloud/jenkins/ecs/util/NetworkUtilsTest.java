package com.alibabacloud.jenkins.ecs.util;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by kunlun.ykl on 2020/11/20.
 */
public class NetworkUtilsTest {

    @Test
    public void autoGenerateSubnetTest() {
        List<String> otherVswCidrBlocks = Lists.newArrayList();
        //多个子网测试
        otherVswCidrBlocks.add("172.16.0.0/24");
//        otherVswCidrBlocks.add("172.16.1.0/23");
        otherVswCidrBlocks.add("172.16.1.0/24");
        String s = NetworkUtils.autoGenerateSubnet("172.16.0.0/12", otherVswCidrBlocks);
        System.out.println(s);
        //一个子网测试
        otherVswCidrBlocks = new ArrayList<>();
        otherVswCidrBlocks.add("192.168.0.0/26");
        s = NetworkUtils.autoGenerateSubnet("192.168.0.0/24", otherVswCidrBlocks);
        System.out.println(s);
        //父网测试
        otherVswCidrBlocks = new ArrayList<>();
        otherVswCidrBlocks.add("192.0.0.0/4");
        s = NetworkUtils.autoGenerateSubnet("192.168.0.0/12", otherVswCidrBlocks);
        System.out.println(s);
        //不相关网络测试
        otherVswCidrBlocks = new ArrayList<>();
        otherVswCidrBlocks.add("172.16.0.128/16");
        s = NetworkUtils.autoGenerateSubnet("192.168.0.0/12", otherVswCidrBlocks);
        System.out.println(s);
        //空网络测试
        otherVswCidrBlocks = new ArrayList<>();
        s = NetworkUtils.autoGenerateSubnet("192.168.0.0/12", otherVswCidrBlocks);
        System.out.println(s);
        //混合测试
        //不相关网络测试
        otherVswCidrBlocks = new ArrayList<>();
        otherVswCidrBlocks.add("172.16.0.128/16");
        otherVswCidrBlocks.add("192.0.0.0/4");
        otherVswCidrBlocks.add("172.16.0.0/24");
        otherVswCidrBlocks.add("172.16.1.0/23");
        otherVswCidrBlocks.add("192.168.0.0/26");
        s = NetworkUtils.autoGenerateSubnet("172.16.0.0/12", otherVswCidrBlocks);
        System.out.println(s);

    }




    @Test
    public void testContains(){
        String ip1="192.168.0.64/26";
        String ip2="192.168.0.0/26";
        String ip3="192.168.0.0/24";
        System.out.println(NetworkUtils.contains(ip2,ip1));
        System.out.println(NetworkUtils.contains(ip3,ip1));
        String ip4="172.16.0.129/32";
        String ip5="172.16.0.0/12";
        System.out.println(NetworkUtils.contains(ip5,ip4));
    }
}
