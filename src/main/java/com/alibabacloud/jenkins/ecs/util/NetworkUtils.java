package com.alibabacloud.jenkins.ecs.util;

import java.util.List;

import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by kunlun.ykl on 2020/11/20.
 */
@Slf4j
public class NetworkUtils {
    /**
     * 自动将cidrBlock划分为count个子网, 默认取第一个子网
     *
     * @return
     */
    public static String autoGenerateSubnet(String cidrBlock, int count) {
        int rightShift = (int)(Math.log(count) / Math.log(2));
        List<String> cidrBlocks = Splitter.on('/').splitToList(cidrBlock);

        int tail = Integer.parseInt(cidrBlocks.get(1));
        int newTail = tail + rightShift;
        if (newTail >= 32) {
            log.error("autoGenerateSubnet error. illegal cidrBlock: {} newTail: {}", cidrBlock, count);
            return cidrBlock;
        }

        String header = cidrBlocks.get(0);
        return header + "/" + newTail;
    }
}
