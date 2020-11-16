package com.alibabacloud.jenkins.ecs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.CheckForNull;

import com.alibabacloud.credentials.plugin.auth.AlibabaCredentials;
import com.alibabacloud.credentials.plugin.auth.AlibabaKeyPairUtils;
import com.alibabacloud.credentials.plugin.auth.AlibabaPrivateKey;
import com.alibabacloud.credentials.plugin.util.CredentialsHelper;
import com.alibabacloud.jenkins.ecs.client.AlibabaEcsClient;
import com.alibabacloud.jenkins.ecs.exception.AlibabaEcsException;
import com.alibabacloud.jenkins.ecs.util.AlibabaEcsFactory;

import com.alibabacloud.jenkins.ecs.util.CloudHelper;
import com.aliyuncs.ecs.model.v20140526.DescribeAvailableResourceRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeImagesRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeImagesResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeImagesResponse.Image;
import com.aliyuncs.ecs.model.v20140526.DescribeKeyPairsResponse.KeyPair;
import com.aliyuncs.ecs.model.v20140526.DescribeRegionsResponse.Region;
import com.aliyuncs.ecs.model.v20140526.DescribeSecurityGroupsResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeSecurityGroupsResponse.SecurityGroup;
import com.aliyuncs.ecs.model.v20140526.DescribeVSwitchesResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeVSwitchesResponse.VSwitch;
import com.aliyuncs.ecs.model.v20140526.DescribeVpcsResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeVpcsResponse.Vpc;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Failure;
import hudson.model.ItemGroup;
import hudson.model.Label;
import hudson.model.PeriodicWork;
import hudson.security.ACL;
import hudson.security.Permission;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.FormValidation;
import hudson.util.HttpResponses;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import static hudson.security.Permission.CREATE;
import static hudson.security.Permission.UPDATE;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * Created by kunlun.ykl on 2020/8/24.
 */
@Slf4j
public class AlibabaCloud extends Cloud {

    private transient ReentrantLock followerCountingLock = new ReentrantLock();
    public static final String CLOUD_ID_PREFIX = "ecs-";
    public static final String DEFAULT_REMOTE_FS = "/root";
    public static final String DEFAULT_ECS_REGION = "cn-beijing";

    @CheckForNull
    private String credentialsId;

    @CheckForNull
    private String sshKey;

    private transient AlibabaPrivateKey privateKey;

    private String region;
    private String image;
    private String vpc;
    private String securityGroup;
    private String zone;
    private String vsw;
    private String instanceType;
    private String remoteFs;
    private String initScript;
    private String labelString;
    private int minimumNumberOfInstances;

    private List<AlibabaEcsFollowerTemplate> templates;

    private transient AlibabaEcsClient connection;

    public static final String CLOUD_ID = "Alibaba Cloud ECS";

    @DataBoundConstructor
    public AlibabaCloud(String name, String credentialsId, String sshKey, String region,
                        String image, String vpc, String securityGroup, String zone, String vsw, String instanceType,
                        int minimumNumberOfInstances, String initScript, String labelString, String remoteFs) {
        super(StringUtils.isBlank(name) ? CLOUD_ID : name);
        this.credentialsId = credentialsId;
        this.sshKey = sshKey;
        this.region = region;
        this.image = image;
        if (StringUtils.isNotBlank(remoteFs)) {
            this.remoteFs = remoteFs;
        } else {
            this.remoteFs = DEFAULT_REMOTE_FS;
        }
        connection = this.connect();
        if (StringUtils.isBlank(vpc)) {
            vpc = getOrCreateDefaultVpc(region);
        }
        this.vpc = vpc;
        if (StringUtils.isBlank(securityGroup)) {
            this.securityGroup = createDefaultSecurityGroup(region, vpc);
        } else {
            this.securityGroup = securityGroup;
        }
        // get default zone
        if (StringUtils.isBlank(zone)) {
            zone = getDefaultZone();
        }
        this.zone = zone;
        if (StringUtils.isBlank(vsw)) {
            vsw = getOrCreateDefaultVsw(vpc, zone);
        }
        this.vsw = vsw;
        if (StringUtils.isBlank(instanceType)) {
            instanceType = getDefaultInstanceType(region, zone);
        }
        this.instanceType = instanceType;
        this.minimumNumberOfInstances = minimumNumberOfInstances;
        this.initScript = initScript;
        this.labelString = labelString;

        AlibabaEcsFollowerTemplate template = new AlibabaEcsFollowerTemplate(region, zone, instanceType,
            minimumNumberOfInstances, vsw,
            initScript, labelString, remoteFs);
        templates = Lists.newArrayList(template);
        readResolve();
    }

    private String getDefaultInstanceType(String region, String zone) {
        List<String> instanceTypes = connection.describeInstanceTypes(zone, 2, 8.0f);
        if (instanceTypes.isEmpty()) {
            // TODO: handle exception
            return null;
        }
        // TODO: choose instance type
        return instanceTypes.get(0);
    }

    private String getOrCreateDefaultVsw(String vpc, String zone) {
        List<VSwitch> vsws = connection.describeVsws(zone, vpc);
        if (!vsws.isEmpty()) {
            return vsws.get(0).getVSwitchId();
        }

        Vpc vpcInstance = null;
        if (StringUtils.isNotEmpty(vpc)) {
            vpcInstance = connection.describeVpc(vpc);
            return "";
        }

        if(null == vpcInstance){
            log.error("describe vpc is error");
            return "";
        }
        // TODO: auot generate cidr block
//        String cidrBlock = "172.16.0.0/24";
        String cidrBlock = vpcInstance.getCidrBlock();
        String[] ips = cidrBlock.split("/");
        String subnet = String.valueOf(Integer.parseInt(ips[1])+4);
        log.info("cidrBlock is {}",cidrBlock);
        String newCidrBlock = ips[0]+"/"+subnet;
        String vsw = connection.createVsw(zone, vpc, newCidrBlock);
        // TODO: handle exception
        return vsw;
    }

    private String getDefaultZone() {
        List<String> availableZones = connection.describeAvailableZones();
        if (CollectionUtils.isEmpty(availableZones)) {
            //throw new ClientExcep
            // TODO: 这里确认异常类型
            return null;
        }
        return availableZones.get(0);
    }

    private String getOrCreateDefaultVpc(String region) {
        // 1. get vpc if exists
        List<Vpc> vpcs = connection.describeVpcs();
        if (CollectionUtils.isNotEmpty(vpcs)) {
            Vpc vpc = vpcs.get(0);
            log.info("getOrCreateDefaultVpc use default vpc. region: {} vpc: {}", region, vpc);
            return vpc.getVpcId();
        }

        // 2. create vpc if not exists, use default cidr 172.16.0.0/12
        String cidrBlock = "172.16.0.0/12";
        return connection.createVpc(cidrBlock);
    }

    public int getMinimumNumberOfInstances() {
        return minimumNumberOfInstances;
    }

    public String getInitScript() {
        return initScript;
    }

    public String getLabelString() {
        return labelString;
    }

    private String createDefaultSecurityGroup(String region, String vpcId) {
        // 1. get or create default sg
        String sgId;
        List<SecurityGroup> securityGroups = connection.describeSecurityGroups(vpcId);
        if (CollectionUtils.isEmpty(securityGroups)) {
            sgId = connection.createSecurityGroup(vpcId);
        } else {
            sgId = securityGroups.get(0).getSecurityGroupId();
        }

        if (StringUtils.isBlank(sgId)) {
            log.error("createSecurityGroup error. region: {} vpcId: {}", region, vpcId);
            return null;
        }
        // 2. acl for ssh
        boolean success = connection.authorizeSecurityGroup("tcp", "22/22", sgId, "0.0.0.0/0");
        if (!success) {
            log.error("authorizeSecurityGroup error. region: {} vpcId: {}", region, vpcId);
        }
        return sgId;
    }

    @CheckForNull
    public AlibabaPrivateKey resolvePrivateKey() {
        if (sshKey != null) {
            try {
                BasicSSHUserPrivateKey sshCredential = getSshCredential(sshKey);
                if (null == sshCredential) {
                    throw new AlibabaEcsException("sshCredential  is null");
                }
                AlibabaCredentials credentials = CredentialsHelper.getCredentials(credentialsId);
                KeyPair keyPair = AlibabaKeyPairUtils.find(sshCredential.getPrivateKey(), credentials,
                        getRegion());
                privateKey = new AlibabaPrivateKey(sshCredential.getPrivateKey(), keyPair.getKeyPairName());
            } catch (Exception e) {
                log.error("resolvePrivateKey error. sshKey: {}", sshKey, e);
            }
        }
        return privateKey;
    }

    @CheckForNull
    private static BasicSSHUserPrivateKey getSshCredential(String id) {

        BasicSSHUserPrivateKey credential = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        BasicSSHUserPrivateKey.class, // (1)
                        (ItemGroup) null,
                        null,
                        Collections.emptyList()),
                CredentialsMatchers.withId(id));

        if (credential == null) {
            log.error("getSshCredential error. id: {}", id);
        }
        return credential;
    }

    // 注意, readResolve必须返回this
    protected Object readResolve() {
        this.followerCountingLock = new ReentrantLock();
        for (AlibabaEcsFollowerTemplate template : templates) {
            template.setParent(this);
        }
        resolvePrivateKey();
        connect();
        return this;
    }

    public String getZone() {
        return zone;
    }

    public String getVsw() {
        return vsw;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public String getRegion() {
        if (region == null) {
            region = DEFAULT_ECS_REGION;
        }
        if (region.indexOf('_') > 0) {
            return region.replace('_', '-').toLowerCase(Locale.ENGLISH);
        }
        return region;
    }

    public String getImage() {
        return image;
    }

    public String getSecurityGroup() {
        return securityGroup;
    }

    public AlibabaPrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public Collection<PlannedNode> provision(Label label, int excessWorkload) {
        return null;
    }

    @Override
    public boolean canProvision(Label label) {
        log.debug("canProvision invoked");
        return true;
    }

    public String getVpc() {
        return vpc;
    }

    private static String createCloudId(String cloudName) {
        return CLOUD_ID_PREFIX + cloudName.trim();
    }

    public AlibabaEcsClient connect() {
        if (this.connection != null) {
            return connection;
        }
        return reconnectToAlibabaCloudEcs();
    }

    public AlibabaEcsClient reconnectToAlibabaCloudEcs() {
        synchronized (this) {
            connection = AlibabaEcsFactory.getInstance().connect(getCredentials(), getRegion());
            return connection;
        }
    }

    public AlibabaCredentials getCredentials() {
        return CredentialsHelper.getCredentials(credentialsId);
    }

    @CheckForNull
    public String getCredentialsId() {
        return credentialsId;
    }

    @CheckForNull
    public String getSshKey() {
        return sshKey;
    }

    public String getRemoteFs() {
        return remoteFs;
    }

    @RequirePOST
    public HttpResponse doProvision(@QueryParameter String template) throws Exception {
        log.info("doProvision invoked template: {}", template);
        AlibabaEcsFollowerTemplate alibabaEcsFollowerTemplate = getTemplate();
        final Jenkins jenkinsInstance = Jenkins.get();
        if (jenkinsInstance.isQuietingDown()) {
            throw HttpResponses.error(SC_BAD_REQUEST, "Jenkins instance is quieting down");
        }
        if (jenkinsInstance.isTerminating()) {
            throw HttpResponses.error(SC_BAD_REQUEST, "Jenkins instance is terminating");
        }
        try {
            followerCountingLock.lock();
            // check how many spare followers should provision
            int aliveCount = 0;
            Computer[] computers = jenkinsInstance.getComputers();
            for (Computer computer : computers) {
                if (computer instanceof AlibabaEcsComputer) {
                    AlibabaEcsSpotFollower follower = ((AlibabaEcsComputer) computer).getNode();
                    if (null == follower) {
                        continue;
                    }
                    String templateId = follower.getTemplateId();
                    if (StringUtils.isEmpty(templateId)) {
                        continue;
                    }
                    if (template.equals(templateId)) {
                        aliveCount++;
                    }
                }
            }
            int provisionCount = alibabaEcsFollowerTemplate.getMinimumNumberOfInstances() - aliveCount;
            if (provisionCount <= 0) {
                log.info("no need provision. minimumNumberOfInstances:{} aliveCount:{}",
                    alibabaEcsFollowerTemplate.getMinimumNumberOfInstances(), aliveCount);
                return HttpResponses.redirectViaContextPath("/computer/");
            }
            List<AlibabaEcsSpotFollower> provision = alibabaEcsFollowerTemplate.provision(provisionCount);
            if (CollectionUtils.isEmpty(provision)) {
                throw HttpResponses.error(SC_BAD_REQUEST, "followerTemplate.provision error");
            }
            // 将节点都加入jenkins里
            for (AlibabaEcsSpotFollower alibabaEcsSpotFollower : provision) {
                jenkinsInstance.addNode(alibabaEcsSpotFollower);
            }
        } finally {
            followerCountingLock.unlock();
        }
        return HttpResponses.redirectViaContextPath("/computer/");
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {
        public DescriptorImpl() {
            super();
            load();
        }

        @Override
        public String getDisplayName() {
            return "Alibaba Cloud ECS";
        }

        @RequirePOST
        public FormValidation doCheckCloudName(@QueryParameter String value) {
            Jenkins.get().checkPermission(Permission.CREATE);
            Jenkins.get().checkPermission(Permission.UPDATE);
            try {
                Jenkins.checkGoodName(value);
            } catch (Failure e) {
                return FormValidation.error(e.getMessage());
            }

            //String cloudId = createCloudId(value);
            int found = 0;
            for (Cloud c : Jenkins.get().clouds) {
                if (c.name.equals(value)) {
                    found++;
                }
            }
            if (found > 1) {
                return FormValidation.error("Duplicate Cloud Name");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            log.info("configure invoked");
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems() {
            Jenkins.get().checkPermission(Permission.CREATE);
            Jenkins.get().checkPermission(Permission.UPDATE);
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withMatching(
                            CredentialsMatchers.always(),
                            CredentialsProvider.lookupCredentials(AlibabaCredentials.class,
                                    Jenkins.get(),
                                    ACL.SYSTEM,
                                    Collections.emptyList()));
        }

        public ListBoxModel doFillSshKeyItems(@QueryParameter String sshKey) {
            StandardListBoxModel result = new StandardListBoxModel();

            return result
                    .includeMatchingAs(Jenkins.getAuthentication(), Jenkins.get(), BasicSSHUserPrivateKey.class,
                            Collections.<DomainRequirement>emptyList(), CredentialsMatchers.always())
                    .includeMatchingAs(ACL.SYSTEM, Jenkins.get(), BasicSSHUserPrivateKey.class,
                            Collections.<DomainRequirement>emptyList(), CredentialsMatchers.always())
                    .includeCurrentValue(sshKey);
        }

        @RequirePOST
        public FormValidation doTestConnection(@QueryParameter String credentialsId, @QueryParameter String sshKey,
                                               @QueryParameter String region) {
            if(!Jenkins.get().hasPermission(CREATE) && Jenkins.get().hasPermission(UPDATE)){
                return FormValidation.error("permission is error");
            }

            if (StringUtils.isBlank(credentialsId)) {
                return FormValidation.error("Credentials not specificed");
            }
            if (StringUtils.isBlank(region)) {
                region = DEFAULT_ECS_REGION;
            }
            // 1. 校验AK/SK是否正确
            AlibabaCredentials credentials = CredentialsHelper.getCredentials(credentialsId);
            if (credentials == null) {
                log.error(
                        "doTestConnection error. credentials not found. region: {} credentialsId: {}",
                        DEFAULT_ECS_REGION, credentialsId);
                return FormValidation.error("Credentials not found");
            }
            AlibabaEcsClient client = new AlibabaEcsClient(credentials, region);
            List<Region> regions = client.describeRegions();
            if (CollectionUtils.isEmpty(regions)) {
                return FormValidation.error("Illegal ak/sk: " + credentialsId);
            }

            if (StringUtils.isBlank(sshKey)) {
                return FormValidation.error("SSH PrivateKey not specificed");
            }

            try {
                // 2. 校验SSHKey是否存在
                BasicSSHUserPrivateKey sshCredential = getSshCredential(sshKey);
                KeyPair keyPair = AlibabaKeyPairUtils.find(sshCredential.getPrivateKey(), credentials,
                        region);
                if (null == keyPair) {
                    return FormValidation.error("Illegal SSH PrivateKey: " + sshKey);
                }
                return FormValidation.ok("connection ok");
            } catch (Exception e) {
                log.error("SSH PrivateKey validate error", e);
            }
            return FormValidation.error("SSH PrivateKey validate error");
        }

        @RequirePOST
        public ListBoxModel doFillRegionItems(@QueryParameter String credentialsId) {
            Jenkins.get().checkPermission(Permission.CREATE);
            Jenkins.get().checkPermission(Permission.UPDATE);
            ListBoxModel model = new ListBoxModel();
            try {
                AlibabaCredentials credentials = CredentialsHelper.getCredentials(credentialsId);
                if (credentials == null) {
                    log.error(
                            "doFillImageItems error. credentials not found. region: {} credentialsId: {}",
                            DEFAULT_ECS_REGION, credentialsId);
                    return model;
                }
                AlibabaEcsClient client = new AlibabaEcsClient(credentials, DEFAULT_ECS_REGION);
                List<Region> regions = client.describeRegions();
                for (Region region : regions) {
                    model.add(region.getLocalName(), region.getRegionId());
                }
            } catch (Exception ex) {
                // Ignore, as this may happen before the credentials are specified
            }
            return model;
        }

        @RequirePOST
        public ListBoxModel doFillImageItems(@QueryParameter String credentialsId, @QueryParameter String region) {
            Jenkins.get().checkPermission(Permission.CREATE);
            Jenkins.get().checkPermission(Permission.UPDATE);
            ListBoxModel model = new ListBoxModel();
            if (StringUtils.isBlank(credentialsId) || StringUtils.isBlank(region)) {
                return model;
            }
            try {
                AlibabaCredentials credentials = CredentialsHelper.getCredentials(credentialsId);
                if (credentials == null) {
                    log.error(
                            "doFillImageItems error. credentials not found. region: {} credentialsId: {}",
                            region, credentialsId);
                    return model;
                }
                AlibabaEcsClient client = new AlibabaEcsClient(credentials, region);
                DescribeImagesRequest req = new DescribeImagesRequest();
                req.setOSType("linux");
                req.setStatus("Available");
                List<Image> images = client.describeImages(req);
                for (DescribeImagesResponse.Image image : images) {
                    model.add(image.getImageName(), image.getImageId());
                }
            } catch (Exception ex) {
                // Ignore, as this may happen before the credentials are specified
            }
            return model;
        }

        @RequirePOST
        public ListBoxModel doFillKeyPairItems() {
            Jenkins.get().checkPermission(Permission.CREATE);
            Jenkins.get().checkPermission(Permission.UPDATE);
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withMatching(
                            CredentialsMatchers.always(),
                            CredentialsProvider.lookupCredentials(AlibabaCredentials.class,
                                    Jenkins.get(),
                                    ACL.SYSTEM,
                                    Collections.emptyList()));
        }

        @RequirePOST
        public ListBoxModel doFillVpcItems(@QueryParameter String credentialsId, @QueryParameter String region) {
            Jenkins.get().checkPermission(Permission.CREATE);
            Jenkins.get().checkPermission(Permission.UPDATE);
            ListBoxModel model = new ListBoxModel();
            model.add("<not specified>", "");
            if (StringUtils.isBlank(credentialsId) || StringUtils.isBlank(region)) {
                return model;
            }
            try {
                AlibabaCredentials credentials = CredentialsHelper.getCredentials(credentialsId);
                if (credentials == null) {
                    log.error(
                            "doFillVpcItems error. credentials not found. region: {} credentialsId: {}",
                            region, credentialsId);
                    return model;
                }
                AlibabaEcsClient client = new AlibabaEcsClient(credentials, region);
                List<Vpc> vpcs = client.describeVpcs();

                for (DescribeVpcsResponse.Vpc vpc : vpcs) {
                    model.add(vpc.getVpcId(), vpc.getVpcId());
                }
            } catch (Exception ex) {
                // Ignore, as this may happen before the credentials are specified
            }

            return model;
        }

        @RequirePOST
        public ListBoxModel doFillSecurityGroupItems(@QueryParameter String credentialsId,
                                                     @QueryParameter String region, @QueryParameter String vpc) {
            Jenkins.get().checkPermission(Permission.CREATE);
            Jenkins.get().checkPermission(Permission.UPDATE);
            ListBoxModel model = new ListBoxModel();
            model.add("<not specified>", "");
            if (StringUtils.isBlank(credentialsId) || StringUtils.isBlank(region) || StringUtils.isBlank(vpc)) {
                return model;
            }
            try {
                AlibabaCredentials credentials = CredentialsHelper.getCredentials(credentialsId);
                if (credentials == null) {
                    log.error(
                            "doFillSecurityGroupItems error. credentials not found. region: {} vpc: {} credentialsId: {}",
                            region, vpc, credentialsId);
                    return model;
                }
                AlibabaEcsClient client = new AlibabaEcsClient(credentials, region);
                List<SecurityGroup> securityGroups = client.describeSecurityGroups(vpc);
                for (DescribeSecurityGroupsResponse.SecurityGroup securityGroup : securityGroups) {
                    model.add(securityGroup.getSecurityGroupId(), securityGroup.getSecurityGroupId());
                }
            } catch (Exception e) {
                log.error("DescribeSecurityGroups error. region: {} vpc: {}", region, vpc, e);
            }
            return model;
        }

        @RequirePOST
        public ListBoxModel doFillZoneItems(
                @QueryParameter String credentialsId, @QueryParameter String region) {
            Jenkins.get().checkPermission(Permission.CREATE);
            Jenkins.get().checkPermission(Permission.UPDATE);
            ListBoxModel model = new ListBoxModel();
            model.add("<not specified>", "");
            if (StringUtils.isBlank(credentialsId) || StringUtils.isBlank(region)) {
                return model;
            }
            AlibabaCredentials credentials = CredentialsHelper.getCredentials(credentialsId);
            if (credentials == null) {
                log.error(
                        "doFillZoneItems error. credentials not found. region: {} credentialsId: {}",
                        region, credentialsId);
                return model;
            }
            AlibabaEcsClient client = new AlibabaEcsClient(credentials, region);
            List<String> zones = client.describeAvailableZones();
            for (String zone : zones) {
                model.add(zone, zone);
            }
            return model;
        }

        @RequirePOST
        public ListBoxModel doFillVswItems(
                @QueryParameter String credentialsId, @QueryParameter String region, @QueryParameter String vpc,
                @QueryParameter String zone) {
            Jenkins.get().checkPermission(Permission.CREATE);
            Jenkins.get().checkPermission(Permission.UPDATE);
            ListBoxModel model = new ListBoxModel();
            model.add("<not specified>", "");
            if (StringUtils.isBlank(credentialsId) || StringUtils.isBlank(region) || StringUtils.isBlank(zone)) {
                return model;
            }
            AlibabaCredentials credentials = CredentialsHelper.getCredentials(credentialsId);
            if (credentials == null) {
                log.error(
                        "doFillVswItems error. credentials not found. region: {} credentialsId: {}",
                        region, credentialsId);
                return model;
            }
            AlibabaEcsClient client = new AlibabaEcsClient(credentials, region);
            List<VSwitch> vSwitches = client.describeVsws(zone, vpc);
            for (DescribeVSwitchesResponse.VSwitch vsw : vSwitches) {
                model.add(vsw.getVSwitchId(), vsw.getVSwitchId());
            }
            return model;
        }

        @RequirePOST
        public ListBoxModel doFillInstanceTypeItems(@QueryParameter String credentialsId, @QueryParameter String region,
                                                    @QueryParameter String zone) {
            Jenkins.get().checkPermission(Permission.CREATE);
            Jenkins.get().checkPermission(Permission.UPDATE);
            ListBoxModel items = new ListBoxModel();
            items.add("<not specified>", "");
            if (StringUtils.isBlank(credentialsId) || StringUtils.isBlank(region) || StringUtils.isBlank(zone)) {
                return items;
            }
            AlibabaCredentials credentials = CredentialsHelper.getCredentials(credentialsId);
            if (credentials == null) {
                log.error(
                        "doFillInstanceTypeItems error. credentials not found. region: {} credentialsId: {}",
                        region, credentialsId);
                return items;
            }
            AlibabaEcsClient client = new AlibabaEcsClient(credentials, region);
            DescribeAvailableResourceRequest resourceRequest = new DescribeAvailableResourceRequest();
            resourceRequest.setZoneId(zone);
            resourceRequest.setCores(2);
            resourceRequest.setMemory(8.0f);
            List<String> instanceTypes = client.describeInstanceTypes(zone, 2, 8.0f);
            for (String instanceType : instanceTypes) {
                items.add(instanceType, instanceType);
            }
            return items;
        }

        @RequirePOST
        public FormValidation doDryRun(@QueryParameter String credentialsId, @QueryParameter String sshKey,
                                       @QueryParameter String region) {
            // TODO: use param to dryrun create instance
            return FormValidation.ok();
        }
    }

    public List<AlibabaEcsFollowerTemplate> getTemplates() {
        return this.templates;
    }

    public AlibabaEcsFollowerTemplate getTemplate(String template) {
        for (AlibabaEcsFollowerTemplate alibabaEcsFollowerTemplate : templates) {
            if (alibabaEcsFollowerTemplate.getTemplateId().equals(template)) {
                return alibabaEcsFollowerTemplate;
            }
        }
        return null;
    }

    public AlibabaEcsFollowerTemplate getTemplate() {
        return getTemplate(CloudHelper.getTemplateId(zone, instanceType));
    }

    @Extension
    public static class AlibabaEcsConnectionUpdater extends PeriodicWork {
        @Override
        public long getRecurrencePeriod() {
            return TimeUnit.SECONDS.toMillis(60);
        }

        @Override
        protected void doRun() throws Exception {
            Jenkins instance = Jenkins.get();
            if (instance.clouds == null) {
                log.warn("no clouds found, AlibabaEcsConnectionUpdater skipped");
                return;
            }
            for (Cloud cloud : instance.clouds) {
                if (!(cloud instanceof AlibabaCloud)) {
                    continue;
                }
                AlibabaCloud alibabaCloud = (AlibabaCloud) cloud;
                log.info("Checking Alibaba Cloud Connection on: {}", alibabaCloud.getDisplayName());
                List<Region> regions = Lists.newArrayList();
                if (alibabaCloud.connection != null) {
                    regions = alibabaCloud.connection.describeRegions();
                }
                if (CollectionUtils.isEmpty(regions)) {
                    log.warn("Reconnecting to Alibaba Cloud on: {}", alibabaCloud.getDisplayName());
                    alibabaCloud.reconnectToAlibabaCloudEcs();
                }
            }
        }
    }
}
