package com.alibabacloud.jenkins.ecs.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by kunlun.ykl on 2020/11/20.
 */
public class NetworkUtilsTest {

    @Test
    public void autoGenerateSubnetTest() {
        String s = NetworkUtils.autoGenerateSubnet("192.168.0.0/24", 2);
        assertEquals("192.168.0.0/25", s);

        s = NetworkUtils.autoGenerateSubnet("192.168.0.0/24", 4);
        assertEquals("192.168.0.0/26", s);
    }
}
