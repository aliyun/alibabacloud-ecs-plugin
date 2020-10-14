package com.alibabacloud.credentials.plugin.util;

import com.alibabacloud.credentials.plugin.auth.AlibabaCredentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import java.util.Collections;

/**
 * Created by kunlun.ykl on 2020/8/26.
 */
public class CredentialsHelper {
    @CheckForNull
    public static AlibabaCredentials getCredentials(@CheckForNull String credentialsId) {
        if (StringUtils.isBlank(credentialsId)) {
            return null;
        }
        return CredentialsMatchers.firstOrNull(
            CredentialsProvider.lookupCredentials(AlibabaCredentials.class, Jenkins.get(),
                ACL.SYSTEM, Collections.emptyList()),
            CredentialsMatchers.withId(credentialsId));
    }

}
