package com.alibabacloud.jenkins.ecs.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Created by kunlun.ykl on 2020/11/20.
 */
@Slf4j
public class NetworkUtils {

    /**
     * @param vpcCidrBlock       172.16.0.0/12
     * @param otherVswCidrBlocks 172.16.0.0/24,172.16.1.0/24
     * @return 172.16.2.0/24, 172.16.255.0/24
     */

    public static String autoGenerateSubnet(String vpcCidrBlock, List<String> otherVswCidrBlocks) {

        List<String> cidrBlocks = Lists.newArrayList(Splitter.on("/").split(vpcCidrBlock));
        int vpcCidrBlockTail = Integer.parseInt(cidrBlocks.get(1));
        String head = cidrBlocks.get(0);

        if (vpcCidrBlockTail >= 32) {
            log.error("autoGenerateSubnet error. illegal cidrBlock: {} vpcCidrBlockTail: {}", vpcCidrBlock, vpcCidrBlockTail);
            return vpcCidrBlock;
        }
        if (vpcCidrBlockTail <= 16) {
            vpcCidrBlockTail = 16;
        }
        String newIp = head + "/" + vpcCidrBlockTail;
        String result = "";
        Set<IPAddress> subnets = Sets.newTreeSet();
        log.info("-------------------newIp:{}", newIp);

        //排除非子网内容
        List<String> subNetworkList = Lists.newArrayList();
        for (String otherCidrBlocks : otherVswCidrBlocks) {
            if (contains(vpcCidrBlock, otherCidrBlocks)) {
                subNetworkList.add(otherCidrBlocks);
            }
        }
        for (String subNetwork : subNetworkList) {
            ArrayList<IPAddress> addresses = exclude(newIp, subNetwork);
            subnets.addAll(addresses);
        }
        if (subnets.isEmpty()) {
            subnets.addAll(adjustBlock(newIp, 1));
        }
        //排除原来子网一样的网段
        List<IPAddress> existIps = Lists.newArrayList();
        for (IPAddress address : subnets) {
            for(String subnet : otherVswCidrBlocks){
                if(address.toString().equals(subnet)){
                    existIps.add(address);
                }
            }
        }
        //移除存在的子网
        subnets.removeAll(existIps);
        for(IPAddress addr : subnets){
            result = addr.toString();
            break;
        }

        return result;
    }

    static TreeSet<IPAddress> adjustBlock(String original, int bitShift) {
        IPAddress subnet = new IPAddressString(original).getAddress();
        IPAddress newSubnets = subnet.setPrefixLength(subnet.getPrefixLength() +
                bitShift, false);
        TreeSet<IPAddress> subnetSet = new TreeSet<>();
        Iterator<? extends IPAddress> iterator = newSubnets.prefixBlockIterator();
        iterator.forEachRemaining(subnetSet::add);

        return subnetSet;
    }


    static ArrayList<IPAddress> exclude(String addrStr, String sub) {
        IPAddress one = new IPAddressString(addrStr).getAddress();
        IPAddress two = new IPAddressString(sub).getAddress();
        IPAddress result[] = one.subtract(two);
        ArrayList<IPAddress> blockList = new ArrayList<>();
        for (IPAddress addr : result) {
            blockList.addAll(Arrays.asList(addr.spanWithPrefixBlocks()));
        }
        return blockList;
    }

    public static Boolean contains(String network, String address) {
        IPAddressString one = new IPAddressString(network);
        IPAddressString two = new IPAddressString(address);
        return one.contains(two);
    }
}


