package com.alibabacloud.credentials.plugin.auth;

import hudson.util.Secret;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Created by kunlun.ykl on 2020/8/27.
 */
@Slf4j
public class AlibabaPrivateKey {
    private final Secret privateKey;
    private final String keyPairName;
    private final String pfp;

    public AlibabaPrivateKey(String privateKey, String keyPairName) {
        this.privateKey = Secret.fromString(privateKey.trim());
        this.pfp = AlibabaKeyPairUtils.getPublicFingerprint(privateKey);
        this.keyPairName = keyPairName;
    }

    public String getPrivateKey() {
        return privateKey.getPlainText();
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    @Restricted(NoExternalUse.class)
    public Secret getPrivateKeySecret() {
        return privateKey;
    }
}
