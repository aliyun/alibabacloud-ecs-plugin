package com.alibabacloud.credentials.plugin.auth;


import com.alibabacloud.credentials.plugin.client.AlibabaClient;
import com.aliyuncs.ecs.model.v20140526.DescribeKeyPairsResponse.KeyPair;
import jenkins.bouncycastle.api.PEMEncodable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

/**
 * Created by kunlun.ykl on 2020/9/21.
 */
@Slf4j
public class AlibabaKeyPairUtils {

    public static String getPublicFingerprint(String pemData) {
        if (pemData == null || pemData.isEmpty()) {
            log.error("This private key cannot be empty");
            return null;
        }

        java.security.KeyPair kp = null;
        try {
            kp = PEMEncodable.decode(pemData).toKeyPair();
        } catch (IOException e) {
            log.error("This private key is password protected, which isn't supported yet");
        } catch (UnrecoverableKeyException e) {
            log.error("This private key is password protected, which isn't supported yet");
        }
        if(kp == null ){
            return null;
        }
        RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
        byte[] keyType = new byte[0];
        try {
            keyType = "ssh-rsa".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("This private key is password protected, which isn't supported yet");
        }
        byte[] expBlob = publicKey.getPublicExponent().toByteArray();
        byte[] modBlob = publicKey.getModulus().toByteArray();
        byte[] authKeyBlob = new byte[3 * 4 + keyType.length + expBlob.length + modBlob.length];

        byte[] lenArray = BigInteger.valueOf(keyType.length).toByteArray();
        System.arraycopy(lenArray, 0, authKeyBlob, 4 - lenArray.length, lenArray.length);
        System.arraycopy(keyType, 0, authKeyBlob, 4, keyType.length);

        lenArray = BigInteger.valueOf(expBlob.length).toByteArray();
        System.arraycopy(lenArray, 0, authKeyBlob, 4 + keyType.length + 4 - lenArray.length, lenArray.length);
        System.arraycopy(expBlob, 0, authKeyBlob, 4 + (4 + keyType.length), expBlob.length);

        lenArray = BigInteger.valueOf(modBlob.length).toByteArray();
        System.arraycopy(lenArray, 0, authKeyBlob, 4 + expBlob.length + 4 + keyType.length + 4 - lenArray.length,
            lenArray.length);
        System.arraycopy(modBlob, 0, authKeyBlob, 4 + (4 + expBlob.length + (4 + keyType.length)), modBlob.length);
        String pubKeyBody = null;
        try {
            pubKeyBody = new String(Base64.encodeBase64(authKeyBlob),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("This private key is password protected, which isn't supported yet");
        }
        byte[] bytes = Base64.decodeBase64(pubKeyBody);
        MessageDigest messageDigest = null;
        String pubKeyFingerPrint="";
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(bytes);
            byte[] resultByteArray = messageDigest.digest();
            pubKeyFingerPrint = Hex.encodeHexString(resultByteArray);
        } catch (NoSuchAlgorithmException e) {
            log.error("This private key is password protected, which isn't supported yet");
        }

        return pubKeyFingerPrint;
    }

    /**
     * Finds the {@link KeyPair} that corresponds to this key in ECS.
     */
    public static KeyPair find(String pemData, AlibabaCredentials credentials, String regionNo) {
        String pfp = getPublicFingerprint(pemData);
        if (StringUtils.isBlank(pfp)) {
            log.error("getPublicFingerprint error");
            return null;
        }
        AlibabaClient client = new AlibabaClient(credentials, regionNo);
        List<KeyPair> keyPairs = client.describeKeyPairs(null, pfp);
        if (CollectionUtils.isEmpty(keyPairs)) {
            log.error("find keyPairs error. regionNo: {} pfp: {}", regionNo, pfp);
            return null;
        }
        return keyPairs.get(0);
    }

    /**
     * Is this file really a private key?
     */
    public static boolean isPrivateKey(String pemData) throws IOException {
        BufferedReader br = new BufferedReader(new StringReader(pemData));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.equals("-----BEGIN RSA PRIVATE KEY-----")) { return true; }
        }
        return false;
    }

}
