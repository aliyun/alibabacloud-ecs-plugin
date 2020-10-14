package com.alibabacloud.credentials.plugin.auth;

import com.alibabacloud.credentials.plugin.client.AlibabaClient;
import com.aliyuncs.auth.AlibabaCloudCredentials;
import com.aliyuncs.ecs.model.v20140526.DescribeRegionsResponse;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import static com.cloudbees.plugins.credentials.CredentialsScope.SYSTEM;
import java.util.List;
import java.util.UUID;


/**
 * Created by kunlun.ykl on 2020/8/26.
 */
@NameWith(
    value = AlibabaCredentials.NameProvider.class,
    priority = 1
)
@Slf4j
public class AlibabaCredentials extends BaseStandardCredentials implements AlibabaCloudCredentials {
    private final String accessKey;
    private final Secret secretKey;
    public static final String DEFAULT_ECS_REGION = "cn-beijing";

    public AlibabaCredentials(@CheckForNull String accessKey, @CheckForNull String secretKey) {
        super(CredentialsScope.GLOBAL, UUID.randomUUID().toString(), "test");
        this.accessKey = accessKey;
        this.secretKey = Secret.fromString(secretKey);
    }

    @DataBoundConstructor
    public AlibabaCredentials(@CheckForNull CredentialsScope scope, @CheckForNull String id,
                              @CheckForNull String accessKey, @CheckForNull String secretKey,
                              @CheckForNull String description) {
        super(scope, id, description);
        this.accessKey = accessKey;
        this.secretKey = Secret.fromString(secretKey);
    }

    public String getAccessKey() {
        return accessKey;
    }

    public Secret getSecretKey() {
        return secretKey;
    }

    public String getDisplayName() {
        return accessKey;
    }

    @Override
    public CredentialsScope getScope() {
        return SYSTEM;
    }

    @Override
    public String getAccessKeyId() {
        return accessKey;
    }

    @Override
    public String getAccessKeySecret() {
        return secretKey.getPlainText();
    }

    @Extension
    public static class DescriptorImpl extends CredentialsDescriptor {

        @Override
        public String getDisplayName() {
            return "Alibaba Cloud Credentials";
        }

        public FormValidation doCheckSecretKey(@QueryParameter("accessKey") String accessKey,
                                               @QueryParameter String value) {
            if (StringUtils.isBlank(accessKey) && StringUtils.isBlank(value)) {
                return FormValidation.ok();
            }
            if (StringUtils.isBlank(accessKey)) {
                return FormValidation.error("Illegal Access Key");
            }
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Illegal Secret Key");
            }

            AlibabaCloudCredentials credentials = new AlibabaCredentials(accessKey, value);
            AlibabaClient client = new AlibabaClient(credentials, DEFAULT_ECS_REGION);
            List<DescribeRegionsResponse.Region> regions = client.describeRegions();
            if (CollectionUtils.isEmpty(regions)) {
                return FormValidation.error("Illegal ak/sk");
            }
            return FormValidation.ok();
        }
    }
}
