/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.MemberImpl;
import com.hazelcast.nio.Address;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;

@RunWith(HazelcastSerialClassRunner.class)
@Category({QuickTest.class})
public class PublicAddressTest {

    public static final int DEFAULT_PORT = 5701;

    @After
    public void cleanup() {
        Hazelcast.shutdownAll();
    }

    @Test
    public void testUseDefaultPortWhenLoopback() throws UnknownHostException {
        Config config = new Config();
        config.getNetworkConfig().setPublicAddress("127.0.0.1");
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);

        assertEquals(new Address("127.0.0.1", DEFAULT_PORT), getAddress(instance));
    }

    @Test
    public void testUseDefaultPortWhenLocalhost() throws UnknownHostException {
        Config config = new Config();
        config.getNetworkConfig().setPublicAddress("localhost");
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);

        assertEquals(new Address("127.0.0.1", DEFAULT_PORT), getAddress(instance));
    }

    @Test
    public void testUseSpecifiedHost() throws UnknownHostException {
        Config config = new Config();
        config.getNetworkConfig().setPublicAddress("www.example.org");
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);

        assertEquals(new Address("www.example.org", DEFAULT_PORT), getAddress(instance));
    }

    @Test
    public void testUseSpecifiedHostAndPort() throws UnknownHostException {
        Config config = new Config();
        config.getNetworkConfig().setPublicAddress("www.example.org:6789");
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);

        assertEquals(new Address("www.example.org", 6789), getAddress(instance));
    }

    @Test
    public void testUseSpecifiedHostAndPortViaProperty() throws UnknownHostException {
        Config config = new Config();
        config.setProperty("hazelcast.local.publicAddress", "192.168.1.1:6789");
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);

        assertEquals(new Address("192.168.1.1", 6789), getAddress(instance));
    }

    @Test(expected = HazelcastException.class)
    public void testInvalidPublicAddress() {
        Config config = new Config();
        config.getNetworkConfig().setPublicAddress("invalid");

        Hazelcast.newHazelcastInstance(config);
    }

    @Test
    public void testUseBindAddressWhenNoPublicAddressGiven() throws UnknownHostException {
        Config config = new Config();
        config.getNetworkConfig().setPort(5705);
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);

        assertEquals(new Address("127.0.0.1", 5705), getAddress(instance));
    }

    private Address getAddress(HazelcastInstance instance) {
        return ((MemberImpl)instance.getCluster().getLocalMember()).getAddress();
    }
}
