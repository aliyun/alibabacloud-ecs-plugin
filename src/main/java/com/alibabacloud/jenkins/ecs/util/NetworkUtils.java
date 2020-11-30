package com.alibabacloud.jenkins.ecs.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Created by kunlun.ykl on 2020/11/20.
 */
@Slf4j
public class NetworkUtils {

    /**
     * 生成子网，排除已划分的子网
     *
     * @param vpcCidrBlock       172.16.0.0/12
     * @param otherVswCidrBlocks 172.16.0.0/24,172.16.1.0/24
     * @return 172.16.2.0/24, 172.16.255.0/24
     */

    public static String autoGenerateSubnet(String vpcCidrBlock, List<String> otherVswCidrBlocks) {
        log.info("-------------------vpcCidrBlock:{},otherVswCidrBlocks:{}", vpcCidrBlock, otherVswCidrBlocks);
        List<String> cidrBlocks = Lists.newArrayList(Splitter.on("/").split(vpcCidrBlock));
        int vpcCidrBlockTail = Integer.parseInt(cidrBlocks.get(1));
        String head = cidrBlocks.get(0);

        if (vpcCidrBlockTail >= 32) {
            log.error("autoGenerateSubnet error. illegal cidrBlock: {} vpcCidrBlockTail: {}", vpcCidrBlock, vpcCidrBlockTail);
            return vpcCidrBlock;
        }

        if (vpcCidrBlockTail < 16) {
            vpcCidrBlockTail = 15;
        }

        String newIp = head + "/" + vpcCidrBlockTail;

        String result = "";
        Set<IPAddress> subnets = Sets.newTreeSet();

        if (otherVswCidrBlocks.isEmpty()) {
            Integer bitshift = 1;
            subnets.addAll(adjustBlock(newIp, bitshift));
        } else {
            List<String> parentNetworkList = Lists.newArrayList();
            //   移除本网段的父类网段
            for (String otherCidrBlock : otherVswCidrBlocks) {
                if (contains(otherCidrBlock, newIp)) {
                    parentNetworkList.add(otherCidrBlock);
                }
            }
            if (!parentNetworkList.isEmpty()) {
                otherVswCidrBlocks.removeAll(parentNetworkList);
            }
            //排除非子网内容
            List<String> subNetworkList = Lists.newArrayList();
            for (String otherCidrBlock : otherVswCidrBlocks) {
                if (contains(newIp, otherCidrBlock)) {
                    subNetworkList.add(otherCidrBlock);
                }
            }
            for (String subNetwork : subNetworkList) {
                List<IPAddress> addresses = exclude(newIp, subNetwork);
                subnets.addAll(addresses);
            }
            //排除原来子网一样的网段
            List<IPAddress> existIps = Lists.newArrayList();
            for (IPAddress address : subnets) {
                for (String subnet : otherVswCidrBlocks) {
                    if (parentOrSubNetwork(address.toString(), subnet)) {
                        existIps.add(address);
                    }
                }
            }
            //移除存在的子网
            subnets.removeAll(existIps);
        }

        for (IPAddress addr : subnets) {
            if (Integer.parseInt(Lists.newArrayList(Splitter.on("/").split(addr.toString())).get(1)) >= 16) {
                result = addr.toString();
                break;
            }
        }

        if (StringUtils.isEmpty(result)) {
            log.error("autoGenerateSubnet error. Subnet segment exhausted. otherVswCidrBlocks :{}", otherVswCidrBlocks);
            return vpcCidrBlock;
        }
        log.info("autoGenerateSubnet result : {}",result);
        return result;
    }

    /**
     * 按照子网掩码位移位数，生成子网
     *
     * @param original
     * @param bitShift
     * @return
     */
    public static Set<IPAddress> adjustBlock(String original, int bitShift) {
        IPAddress subnet = new IPAddressString(original).getAddress();
        IPAddress newSubnets = subnet.setPrefixLength(subnet.getPrefixLength() +
                bitShift, false);
        TreeSet<IPAddress> subnetSet = new TreeSet<>();
        Iterator<? extends IPAddress> iterator = newSubnets.prefixBlockIterator();
        iterator.forEachRemaining(subnetSet::add);

        return subnetSet;
    }

    /**
     * 划分子网，排除已划分的子网
     *
     * @param addrStr
     * @param sub
     * @return
     */
    public static List<IPAddress> exclude(String addrStr, String sub) {
        IPAddress one = new IPAddressString(addrStr).getAddress();
        IPAddress two = new IPAddressString(sub).getAddress();
        List<IPAddress> result = Arrays.asList(one.subtract(two));
        ArrayList<IPAddress> blockList = new ArrayList<>();
        for (IPAddress addr : result) {
            blockList.addAll(Arrays.asList(addr.spanWithPrefixBlocks()));
        }
        return blockList;
    }

    /**
     * 判断子网的包含关系，包含返回true，否则返回false
     *
     * @param parentNetwork
     * @param childNetwork
     * @return
     */
    public static Boolean contains(String parentNetwork, String childNetwork) {
        IPAddressString one = new IPAddressString(parentNetwork);
        IPAddressString two = new IPAddressString(childNetwork);

        return one.contains(two);
    }

    /**
     * 判断两个网是否是父子网关系
     *
     * @param net1
     * @param net2
     * @return
     */
    public static Boolean parentOrSubNetwork(String net1, String net2) {
        return contains(net1, net2) || contains(net2, net1);
    }

}


