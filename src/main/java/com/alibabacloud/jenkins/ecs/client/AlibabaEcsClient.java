package com.alibabacloud.jenkins.ecs.client;

import java.util.List;

import javax.annotation.Nullable;

import com.alibaba.fastjson.JSON;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.auth.AlibabaCloudCredentials;
import com.aliyuncs.ecs.model.v20140526.*;
import com.aliyuncs.ecs.model.v20140526.DescribeAvailableResourceResponse.AvailableZone;
import com.aliyuncs.ecs.model.v20140526.DescribeAvailableResourceResponse.AvailableZone.AvailableResource;
import com.aliyuncs.ecs.model.v20140526.DescribeAvailableResourceResponse.AvailableZone.AvailableResource.SupportedResource;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesResponse.Instance;
import com.aliyuncs.ecs.model.v20140526.DescribeKeyPairsResponse.KeyPair;
import com.aliyuncs.ecs.model.v20140526.DescribeRegionsResponse.Region;
import com.aliyuncs.ecs.model.v20140526.DescribeSecurityGroupsResponse.SecurityGroup;
import com.aliyuncs.ecs.model.v20140526.DescribeVSwitchesResponse.VSwitch;
import com.aliyuncs.ecs.model.v20140526.DescribeVpcsResponse.Vpc;
import com.aliyuncs.ecs.model.v20140526.DescribeZonesResponse.Zone;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Created by kunlun.ykl on 2020/8/26.
 */
@Slf4j
public class AlibabaEcsClient {

    private IAcsClient client;
    private String regionNo;
    private static Integer MAX_PAGE_SIZE = 50;
    private static Integer INIT_PAGE_NUMBER = 1;

    public AlibabaEcsClient(AlibabaCloudCredentials credentials, String regionNo) {
        IClientProfile profile = DefaultProfile.getProfile(regionNo,
                credentials.getAccessKeyId(),
                credentials.getAccessKeySecret());
        this.client = new DefaultAcsClient(profile);
        this.regionNo = regionNo;
        log.info("AlibabaEcsClient init success. regionNo: {}", regionNo);
    }

    public List<Region> describeRegions() {
        try {
            DescribeRegionsRequest request = new DescribeRegionsRequest();
            request.setSysRegionId(regionNo);
            DescribeRegionsResponse acsResponse = client.getAcsResponse(request);
            if (CollectionUtils.isEmpty(acsResponse.getRegions())) {
                return Lists.newArrayList();
            }
            return acsResponse.getRegions();
        } catch (Exception e) {
            log.error("describeRegions error.", e);
        }
        return Lists.newArrayList();
    }

    public List<DescribeImagesResponse.Image> describeImages(DescribeImagesRequest request) {
        try {
            request.setSysRegionId(regionNo);
            DescribeImagesResponse acsResponse = client.getAcsResponse(request);
            if (CollectionUtils.isEmpty(acsResponse.getImages())) {
                return Lists.newArrayList();
            }
            return acsResponse.getImages();
        } catch (Exception e) {
            log.error("describeImages error.", e);
        }
        return Lists.newArrayList();
    }

    public List<Vpc> describeVpcs() {
        DescribeVpcsRequest request = new DescribeVpcsRequest();
        request.setPageNumber(INIT_PAGE_NUMBER);
        request.setPageSize(MAX_PAGE_SIZE);
        request.setSysRegionId(regionNo);
        List<Vpc> vpcList = Lists.newArrayList();
        DescribeVpcsResponse acsResponse = new DescribeVpcsResponse();
        acsResponse.setVpcs(vpcList);
        do {
            try {
                acsResponse = client.getAcsResponse(request);
                if (null == acsResponse || CollectionUtils.isEmpty(acsResponse.getVpcs())) {
                    break;
                }
                vpcList.addAll(acsResponse.getVpcs());
            } catch (Exception e) {
                log.error("describeVpcs error.regionId: {}", regionNo, e);
            }
            request.setPageNumber(request.getPageNumber() + 1);
        } while (acsResponse.getVpcs().size() == MAX_PAGE_SIZE);

        return vpcList;
    }

    public Vpc describeVpc(String vpcId) {
        if (StringUtils.isEmpty(vpcId)) {
            log.error("vpcId is not null");
        }
        DescribeVpcsRequest request = new DescribeVpcsRequest();

        request.setSysRegionId(regionNo);
        request.setVpcId(vpcId);
        DescribeVpcsResponse acsResponse = null;
        Vpc vpcInstance = null;
        try {
            acsResponse = client.getAcsResponse(request);
            if (null != acsResponse && null != acsResponse.getVpcs() && !acsResponse.getVpcs().isEmpty()) {
                vpcInstance = acsResponse.getVpcs().get(0);
            }

        } catch (Exception e) {
            log.error("describeVpc error.regionId is : {},vpcId is :{}", regionNo, vpcId, e);
        }
        return vpcInstance;
    }

    public String createVpc(String cidrBlock) {
        try {
            CreateVpcRequest request = new CreateVpcRequest();
            request.setSysRegionId(regionNo);
            request.setCidrBlock(cidrBlock);
            CreateVpcResponse acsResponse = client.getAcsResponse(request);
            log.info("createVpc success. region: {} cidrBlock: {} vpcId: {}", regionNo, cidrBlock,
                    acsResponse.getVpcId());
            return acsResponse.getVpcId();
        } catch (Exception e) {
            log.error("createVpc error.", e);
        }
        return null;
    }

    public boolean deleteVpc(String vpcId) {
        try {
            DeleteVpcRequest request = new DeleteVpcRequest();
            request.setVpcId(vpcId);
            client.getAcsResponse(request);
            log.info("delete vpc success. vpcId: {}", vpcId);
            return true;
        } catch (Exception e) {
            log.error("delete vpc error. vpcId: {}", vpcId, e);
        }
        return false;
    }

    public List<SecurityGroup> describeSecurityGroups(String vpc) {
        try {
            DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
            request.setSysRegionId(regionNo);
            request.setVpcId(vpc);
            DescribeSecurityGroupsResponse acsResponse = client.getAcsResponse(request);
            if (CollectionUtils.isEmpty(acsResponse.getSecurityGroups())) {
                return Lists.newArrayList();
            }
            return acsResponse.getSecurityGroups();
        } catch (Exception e) {
            log.error("describeSecurityGroups error.", e);
        }
        return Lists.newArrayList();
    }

    public String createSecurityGroup(String vpcId) {
        try {
            CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
            request.setSysRegionId(regionNo);
            request.setVpcId(vpcId);
            CreateSecurityGroupResponse acsResponse = client.getAcsResponse(request);
            if (StringUtils.isBlank(acsResponse.getSecurityGroupId())) {
                return null;
            }
            log.info("createSecurityGroup success. vpcId: {} securityGroupId: {}", vpcId,
                    acsResponse.getSecurityGroupId());
            return acsResponse.getSecurityGroupId();
        } catch (Exception e) {
            log.error("createSecurityGroup error. vpcId: {}", vpcId, e);
        }
        return null;
    }

    public boolean authorizeSecurityGroup(String protocol, String portRange, String securityGroupId,
                                          String sourceCidrIp) {
        try {
            AuthorizeSecurityGroupRequest authRequest = new AuthorizeSecurityGroupRequest();
            authRequest.setSysRegionId(regionNo);
            authRequest.setIpProtocol(protocol);
            authRequest.setPortRange(portRange);
            authRequest.setSecurityGroupId(securityGroupId);
            authRequest.setSourceCidrIp(sourceCidrIp);
            client.getAcsResponse(authRequest);
            log.info("authorizeSecurityGroup success. protocol: {} portRange: {} securityGroupId: {} sourceCidrIp: {}",
                    protocol, portRange, securityGroupId, sourceCidrIp);
            return true;
        } catch (Exception e) {
            log.error("authorizeSecurityGroup error. securityGroupId: {}", securityGroupId, e);
        }
        return false;
    }

    /**
     * use {@linkplain AlibabaEcsClient#describeAvailableZones()} instead
     *
     * @return
     */
    @Deprecated
    public List<Zone> describeZones() {
        try {
            DescribeZonesRequest request = new DescribeZonesRequest();
            request.setSysRegionId(regionNo);
            DescribeZonesResponse acsResponse = client.getAcsResponse(request);
            if (CollectionUtils.isEmpty(acsResponse.getZones())) {
                return Lists.newArrayList();
            }
            return acsResponse.getZones();
        } catch (Exception e) {
            log.error("describeZones error.", e);
        }
        return Lists.newArrayList();
    }

    public List<String> describeAvailableZones() {
        try {
            List<String> zoneIds = Lists.newArrayList();
            DescribeAvailableResourceRequest resourceRequest = new DescribeAvailableResourceRequest();
            resourceRequest.setSysRegionId(regionNo);
            resourceRequest.setDestinationResource("Zone");
            DescribeAvailableResourceResponse acsResponse = client.getAcsResponse(resourceRequest);
            for (AvailableZone availableZone : acsResponse.getAvailableZones()) {
                if (!"Available".equalsIgnoreCase(availableZone.getStatus())) {
                    continue;
                }
                if (!"WithStock".equals(availableZone.getStatusCategory())) {
                    continue;
                }
                zoneIds.add(availableZone.getZoneId());
            }
            return zoneIds;
        } catch (Exception e) {
            log.error("describeAvailableZones error.", e);
        }
        return Lists.newArrayList();
    }

    public List<VSwitch> describeVsws(String zone, String vpc) {
        try {
            DescribeVSwitchesRequest describeZonesRequest = new DescribeVSwitchesRequest();
            describeZonesRequest.setSysRegionId(regionNo);
            if (StringUtils.isNotEmpty(zone)) {
                describeZonesRequest.setZoneId(zone);
            }
            describeZonesRequest.setVpcId(vpc);
            DescribeVSwitchesResponse acsResponse = client.getAcsResponse(describeZonesRequest);
            if (CollectionUtils.isEmpty(acsResponse.getVSwitches())) {
                return Lists.newArrayList();
            }
            return acsResponse.getVSwitches();
        } catch (Exception e) {
            log.error("describeVsws error.", e);
        }
        return Lists.newArrayList();
    }

    public String createVsw(String zone, String vpc, String cidrBlock) {
        try {
            CreateVSwitchRequest createVswRequest = new CreateVSwitchRequest();
            createVswRequest.setSysRegionId(regionNo);
            createVswRequest.setZoneId(zone);
            createVswRequest.setVpcId(vpc);
            createVswRequest.setCidrBlock(cidrBlock);
            CreateVSwitchResponse acsResponse = client.getAcsResponse(createVswRequest);
            log.info("createVsw success. zone: {} vpc: {} cidrBlock: {} vswId: {}",
                    zone, vpc, cidrBlock, acsResponse.getVSwitchId());
            return acsResponse.getVSwitchId();
        } catch (Exception e) {
            log.error("createVsw error. zone: {} vpc: {} cidrBlock: {}", zone, vpc, cidrBlock, e);
        }
        return null;
    }

    public boolean deleteVsw(String vSwitchId) {
        try {
            DeleteVSwitchRequest deleteVSwitchRequest = new DeleteVSwitchRequest();
            deleteVSwitchRequest.setVSwitchId(vSwitchId);
            client.getAcsResponse(deleteVSwitchRequest);
            log.info("deleteVSW success. vswId: {}", vSwitchId);
            return true;
        } catch (Exception e) {
            log.error("deleteVsw error. vswId: {}", vSwitchId, e);
        }
        return false;
    }

    public List<String> describeInstanceTypes(String zone, int core, float memInGb) {
        try {
            DescribeAvailableResourceRequest resourceRequest = new DescribeAvailableResourceRequest();
            resourceRequest.setDestinationResource("InstanceType");
            resourceRequest.setIoOptimized("optimized");
            resourceRequest.setNetworkCategory("vpc");
            resourceRequest.setResourceType("instance");
            resourceRequest.setSpotStrategy("SpotAsPriceGo");
            resourceRequest.setInstanceChargeType("PostPaid");
            resourceRequest.setCores(core);
            resourceRequest.setMemory(memInGb);
            DescribeAvailableResourceResponse acsResponse = client.getAcsResponse(resourceRequest);
            if (CollectionUtils.isEmpty(acsResponse.getAvailableZones())) {
                return Lists.newArrayList();
            }
            List<String> instanceTypes = Lists.newArrayList();

            for (AvailableZone availableZone : acsResponse.getAvailableZones()) {
                if (!zone.equals(availableZone.getZoneId())) {
                    continue;
                }
                for (AvailableResource availableResource : availableZone.getAvailableResources()) {
                    if (!"InstanceType".equals(availableResource.getType())) {
                        continue;
                    }
                    for (SupportedResource supportedResource : availableResource.getSupportedResources()) {
                        if ("Available".equals(supportedResource.getStatus()) && "WithStock".equals(
                                supportedResource.getStatusCategory())) {
                            instanceTypes.add(supportedResource.getValue());
                        }
                    }
                }
            }
            return instanceTypes;
        } catch (Exception e) {
            log.error("describeSpotInstanceTypes error.", e);
        }
        return Lists.newArrayList();
    }

    public List<KeyPair> describeKeyPairs(@Nullable String keyPairName, @Nullable String pfp) {
        try {
            DescribeKeyPairsRequest request = new DescribeKeyPairsRequest();
            request.setSysRegionId(regionNo);
            request.setKeyPairName(keyPairName);
            request.setKeyPairFingerPrint(pfp);
            DescribeKeyPairsResponse acsResponse = client.getAcsResponse(request);
            log.info(JSON.toJSONString(acsResponse));
            List<KeyPair> keyPairs = acsResponse.getKeyPairs();
            if (CollectionUtils.isEmpty(keyPairs)) {
                return Lists.newArrayList();
            }
            return keyPairs;
        } catch (Exception e) {
            log.error("listKeyPairs error", e);
        }
        return Lists.newArrayList();
    }

    public List<Instance> describeInstances(List<String> instanceIds) {
        try {
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            request.setInstanceIds(JSON.toJSONString(instanceIds));
            DescribeInstancesResponse acsResponse = client.getAcsResponse(request);
            if (CollectionUtils.isEmpty(acsResponse.getInstances())) {
                return Lists.newArrayList();
            }
            return acsResponse.getInstances();
        } catch (Exception e) {
            log.error("describeInstances error. instanceIds: {}", JSON.toJSONString(instanceIds), e);
        }
        return Lists.newArrayList();
    }

    public void stopIntance(String instanceId) {
        try {
            StopInstanceRequest request = new StopInstanceRequest();
            request.setInstanceId(instanceId);
            StopInstanceResponse acsResponse = client.getAcsResponse(request);
            log.info("stopInstance success. instanceId: {} response: {}", instanceId, JSON.toJSONString(acsResponse));
        } catch (Exception e) {
            log.error("stopIntance error. instanceId: {}", instanceId, e);
        }
    }

    /**
     * @param instanceId
     */
    public void terminateInstance(String instanceId, boolean force) {
        try {
            DeleteInstanceRequest request = new DeleteInstanceRequest();
            request.setInstanceId(instanceId);
            request.setForce(force);
            DeleteInstanceResponse acsResponse = client.getAcsResponse(request);
            log.info("terminateIntance success. instanceId: {} resp: {}", instanceId, JSON.toJSONString(acsResponse));
        } catch (Exception e) {
            log.error("terminateIntance error. instanceId: {}", instanceId, e);
        }
    }

    public List<String> runInstances(RunInstancesRequest request) {
        try {
            List<RunInstancesRequest.Tag> tags = Lists.newArrayList();
            RunInstancesRequest.Tag tag = new RunInstancesRequest.Tag();
            tag.setKey("CreatedFrom");
            tag.setValue("jenkins-plugin");
            tags.add(tag);
            request.setTags(tags);
            request.setAcceptFormat(FormatType.JSON);
            request.setInstanceChargeType("PostPaid");
            request.setSpotStrategy("SpotAsPriceGo");
            request.setIoOptimized("optimized");

            RunInstancesResponse acsResponse = client.getAcsResponse(request);
            List<String> instanceIdSets = acsResponse.getInstanceIdSets();
            log.info("runInstances success. instanceIdSets: {}", JSON.toJSONString(instanceIdSets));
            return instanceIdSets;
        } catch (Exception e) {
            log.error("runInstances error. request: {}", JSON.toJSONString(request), e);
        }
        return Lists.newArrayList();
    }

    public String allocatePublicIp(String instanceId) {
        try {
            AllocatePublicIpAddressRequest ipRequest = new AllocatePublicIpAddressRequest();
            ipRequest.setInstanceId(instanceId);
            AllocatePublicIpAddressResponse acsResponse = client.getAcsResponse(ipRequest);
            String ipAddress = acsResponse.getIpAddress();
            log.info("allocatePublicIp success. instanceId: {} ipAddress: {}", instanceId, ipAddress);
            return ipAddress;
        } catch (Exception e) {
            log.error("allocatePublicIp error. instanceId: {}", instanceId, e);
        }
        return null;
    }

}
