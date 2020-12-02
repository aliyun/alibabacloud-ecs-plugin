package com.alibabacloud.jenkins.ecs.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by kunlun.ykl on 2020/11/20.
 */
public class NetworkUtilsTest {

    @Test
    public void autoGenerateSubnetTest() {
        List<String> otherVswCidrBlocks = Lists.newArrayList();
        String vpcCidrBlock;
        String subnet;

        // Case 1 ~ 4: Single existing VSW
        // Case 1: VPC-"172.16.0.0/12", other cidrBlocks-{"172.16.0.0/16"}
        vpcCidrBlock = "172.16.0.0/12";
        otherVswCidrBlocks = Lists.newArrayList("172.16.0.0/16");
        checkExpectation(vpcCidrBlock, otherVswCidrBlocks);

        // Case 2: VPC-"192.168.0.0/16", other cidrBlocks-{"192.168.0.0/18"}
        vpcCidrBlock = "192.168.0.0/16";
        otherVswCidrBlocks = Lists.newArrayList("192.168.0.0/18");
        checkExpectation(vpcCidrBlock, otherVswCidrBlocks);

        // Case 3: VPC-"10.0.0.0/8", other cidrBlocks-{"10.0.0.0/16"}
        vpcCidrBlock = "10.0.0.0/8";
        otherVswCidrBlocks = Lists.newArrayList("10.0.0.0/16");
        checkExpectation(vpcCidrBlock, otherVswCidrBlocks);

        // Case 4: VPC-"10.0.0.0/8", other cidrBlocks-{"10.0.0.0/22"}
        vpcCidrBlock = "10.0.0.0/8";
        otherVswCidrBlocks = Lists.newArrayList("10.0.0.0/22");
        checkExpectation(vpcCidrBlock, otherVswCidrBlocks);

        // Case 5 ~ 7: No existing VSWs
        // Case 5: VPC-"192.168.0.0/16", other cidrBlocks-{}
        vpcCidrBlock = "192.168.0.0/16";
        otherVswCidrBlocks = Lists.newArrayList();
        checkExpectation(vpcCidrBlock, otherVswCidrBlocks);

        // Case 6: VPC-"172.16.0.0/12", other cidrBlocks-{}
        vpcCidrBlock = "172.16.0.0/12";
        otherVswCidrBlocks = Lists.newArrayList();
        checkExpectation(vpcCidrBlock, otherVswCidrBlocks);

        // Case 7: VPC-"10.0.0.0/8", other cidrBlocks-{}
        vpcCidrBlock = "10.0.0.0/8";
        otherVswCidrBlocks = Lists.newArrayList();
        checkExpectation(vpcCidrBlock, otherVswCidrBlocks);

        // Case 8 ~ 11: Multiple existing VSWs and there is still available subnet which can be created
        // Case 8: VPC-"172.16.0.0/12", other cidrBlocks-{"172.16.128.0/18","172.16.64.0/18","172.16.32.0/19"}
        vpcCidrBlock = "172.16.0.0/12";
        otherVswCidrBlocks = Lists.newArrayList("172.16.128.0/18", "172.16.64.0/18", "172.16.32.0/19");
        checkExpectation(vpcCidrBlock, otherVswCidrBlocks);

        // Case 9: VPC-"192.168.0.0/16", other cidrBlocks-{"192.168.0.0/18","192.168.144.0/20","192.168.64.0/18"}
        vpcCidrBlock = "192.168.0.0/16";
        otherVswCidrBlocks = Lists.newArrayList("192.168.0.0/18", "192.168.144.0/20", "192.168.64.0/18");
        checkExpectation(vpcCidrBlock, otherVswCidrBlocks);

        // Case 10: VPC-"10.0.0.0/8", other cidrBlocks-{"10.0.0.0/18","10.0.128.0/18"}
        vpcCidrBlock = "10.0.0.0/8";
        otherVswCidrBlocks = Lists.newArrayList("10.0.0.0/18", "10.0.128.0/18");
        checkExpectation(vpcCidrBlock, otherVswCidrBlocks);

        // Case 11: VPC-"10.0.0.0/8", other cidrBlocks-{"10.0.128.0/18","10.0.0.0/18","10.0.192.0/18","10.0.64.0/19"}
        vpcCidrBlock = "10.0.0.0/8";
        otherVswCidrBlocks = Lists.newArrayList("10.0.128.0/18", "10.0.0.0/18", "10.0.192.0/18", "10.0.64.0/19");
        checkExpectation(vpcCidrBlock, otherVswCidrBlocks);

        //Case 12 ~ 13: All subnet possibilities are already occupied
        //Case 12: VPC-"192.168.0.0/16", other cidrBlocks-{"192.168.0.0/17","192.168.128.0/17"}
        vpcCidrBlock = "192.168.0.0/16";
        otherVswCidrBlocks = Lists.newArrayList("192.168.0.0/17", "192.168.128.0/17");
        subnet = NetworkUtils.autoGenerateSubnet(vpcCidrBlock, otherVswCidrBlocks);
        Assert.assertEquals(0, subnet.length());

        //Case 13: VPC-"192.168.0.0/24", other cidrBlocks-{"192.168.0.0/26","192.168.0.128/25","192.168.0.64/26"}
        vpcCidrBlock = "192.168.0.0/24";
        otherVswCidrBlocks = Lists.newArrayList("192.168.0.0/26", "192.168.0.128/25", "192.168.0.64/26");
        subnet = NetworkUtils.autoGenerateSubnet(vpcCidrBlock, otherVswCidrBlocks);
        Assert.assertEquals(0, subnet.length());

    }

    private void checkExpectation(String vpcCidrBlock, List<String> otherVswCidrBlocks) {
        Set<String> otherVswCidrBlockSet = new HashSet<>(otherVswCidrBlocks);
        for (String vsw : otherVswCidrBlockSet) {
            for (String otherVsw : otherVswCidrBlockSet) {
                if (StringUtils.equals(vsw, otherVsw)) {
                    continue;
                }
                Assert.assertFalse(NetworkUtils.contains(vsw, otherVsw));
                Assert.assertFalse(NetworkUtils.contains(otherVsw, vsw));
            }
        }
        String subnet = NetworkUtils.autoGenerateSubnet(vpcCidrBlock, otherVswCidrBlocks);
        Assert.assertTrue(NetworkUtils.contains(vpcCidrBlock, subnet));
        for (String vswCidrBlock : otherVswCidrBlocks) {
            Assert.assertFalse(NetworkUtils.contains(subnet, vswCidrBlock));
        }
    }

    @Test
    public void containTest() {
        // Case 1: Three subnets and they have no overlapping each other
        List<String> subNetNoOverlapping = Lists.newArrayList("192.168.0.0/26", "192.168.0.128/25", "192.168.0.64/26");
        for (String subnetOne : subNetNoOverlapping) {
            for (String subnetTwo : subNetNoOverlapping) {
                if (StringUtils.equals(subnetOne, subnetTwo)) {
                    continue;
                }
                Assert.assertFalse(NetworkUtils.contains(subnetOne, subnetTwo));
            }
        }

        // Case 2: Same subnet
        String subnetSame = "192.168.0.0/26";
        Assert.assertTrue(NetworkUtils.contains(subnetSame, subnetSame));

        // Case 3: Two subnets, one contains another
        String biggerSubnet = "10.0.0.0/8";
        String smallerSubnet = "10.0.0.0/18";
        Assert.assertTrue(NetworkUtils.contains(biggerSubnet, smallerSubnet));
        Assert.assertFalse(NetworkUtils.contains(smallerSubnet, biggerSubnet));
    }
}
