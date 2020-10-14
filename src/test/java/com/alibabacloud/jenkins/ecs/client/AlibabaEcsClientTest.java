package com.alibabacloud.jenkins.ecs.client;

import java.util.List;

import com.alibabacloud.credentials.plugin.auth.AlibabaCredentials;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.ecs.model.v20140526.DescribeKeyPairsRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeKeyPairsResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeKeyPairsResponse.KeyPair;
import com.aliyuncs.exceptions.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by kunlun.ykl on 2020/8/27.
 */
@Slf4j
@PowerMockIgnore(
    {"javax.crypto.*", "org.hamcrest.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest(
    {DefaultAcsClient.class})
public class AlibabaEcsClientTest {
    @Mock
    DefaultAcsClient acsClient;

    @Test
    public void listKeyPairsTest() throws ClientException {
        String ak = "<sample-ak>";
        String sk = "<sample-sk>";
        AlibabaCredentials credentials = new AlibabaCredentials(ak, sk);
        AlibabaEcsClient client = new AlibabaEcsClient(credentials, "cn-beijing");
        Whitebox.setInternalState(client, "client", acsClient);

        DescribeKeyPairsResponse acsResponse = new DescribeKeyPairsResponse();
        List<KeyPair> keyPairs = Lists.newArrayList();
        keyPairs.add(new KeyPair());
        acsResponse.setKeyPairs(keyPairs);
        when(acsClient.getAcsResponse(any(DescribeKeyPairsRequest.class))).thenReturn(acsResponse);

        List<DescribeKeyPairsResponse.KeyPair> keyPairsRes = client.describeKeyPairs("testKeyPairName", null);
        assertNotNull(keyPairsRes);
        assertEquals(1, keyPairsRes.size());
    }
}
