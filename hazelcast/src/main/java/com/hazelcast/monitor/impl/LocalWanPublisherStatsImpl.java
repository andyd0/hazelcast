/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
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


package com.hazelcast.monitor.impl;

import com.eclipsesource.json.JsonObject;
import com.hazelcast.config.WanPublisherState;
import com.hazelcast.monitor.LocalWanPublisherStats;
import com.hazelcast.wan.impl.DistributedServiceWanEventCounters.DistributedObjectWanEventCounters;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import static com.hazelcast.util.JsonUtil.getBoolean;
import static com.hazelcast.util.JsonUtil.getInt;
import static com.hazelcast.util.JsonUtil.getLong;
import static java.util.concurrent.atomic.AtomicLongFieldUpdater.newUpdater;

public class LocalWanPublisherStatsImpl implements LocalWanPublisherStats {

    private static final AtomicLongFieldUpdater<LocalWanPublisherStatsImpl> TOTAL_PUBLISH_LATENCY =
            newUpdater(LocalWanPublisherStatsImpl.class, "totalPublishLatency");
    private static final AtomicLongFieldUpdater<LocalWanPublisherStatsImpl> TOTAL_PUBLISHED_EVENT_COUNT =
            newUpdater(LocalWanPublisherStatsImpl.class, "totalPublishedEventCount");

    private volatile boolean connected;
    private volatile WanPublisherState state;
    private volatile int outboundQueueSize;
    private volatile long totalPublishLatency;
    private volatile long totalPublishedEventCount;
    private volatile Map<String, DistributedObjectWanEventCounters> sentMapEventCounter;
    private volatile Map<String, DistributedObjectWanEventCounters> sentCacheEventCounter;

    @Override
    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public int getOutboundQueueSize() {
        return outboundQueueSize;
    }

    public void setOutboundQueueSize(int outboundQueueSize) {
        this.outboundQueueSize = outboundQueueSize;
    }

    @Override
    public WanPublisherState getPublisherState() {
        return state;
    }

    public void setState(WanPublisherState state) {
        this.state = state;
    }

    @Override
    public long getTotalPublishLatency() {
        return totalPublishLatency;
    }

    @Override
    public long getTotalPublishedEventCount() {
        return totalPublishedEventCount;
    }

    @Override
    public Map<String, DistributedObjectWanEventCounters> getSentMapEventCounter() {
        return sentMapEventCounter;
    }

    public void setSentMapEventCounter(Map<String, DistributedObjectWanEventCounters> sentMapEventCounter) {
        this.sentMapEventCounter = sentMapEventCounter;
    }

    @Override
    public Map<String, DistributedObjectWanEventCounters> getSentCacheEventCounter() {
        return sentCacheEventCounter;
    }

    public void setSentCacheEventCounter(Map<String, DistributedObjectWanEventCounters> sentCacheEventCounter) {
        this.sentCacheEventCounter = sentCacheEventCounter;
    }

    public void incrementPublishedEventCount(long latency) {
        TOTAL_PUBLISHED_EVENT_COUNT.incrementAndGet(this);
        TOTAL_PUBLISH_LATENCY.addAndGet(this, latency);
    }

    @Override
    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.add("isConnected", connected);
        root.add("totalPublishLatencies", totalPublishLatency);
        root.add("totalPublishedEventCount", totalPublishedEventCount);
        root.add("outboundQueueSize", outboundQueueSize);
        root.add("paused", !state.isReplicateEnqueuedEvents());
        root.add("stopped", !state.isEnqueueNewEvents());
        return root;
    }

    @Override
    public void fromJson(JsonObject json) {
        connected = getBoolean(json, "isConnected", false);
        totalPublishLatency = getLong(json, "totalPublishLatencies", -1);
        totalPublishedEventCount = getLong(json, "totalPublishedEventCount", -1);
        outboundQueueSize = getInt(json, "outboundQueueSize", -1);
        final boolean paused = getBoolean(json, "paused");
        final boolean stopped = getBoolean(json, "stopped");
        if (stopped) {
            state = WanPublisherState.STOPPED;
        } else if (paused) {
            state = WanPublisherState.PAUSED;
        } else {
            state = WanPublisherState.REPLICATING;
        }
    }

    @Override
    public String toString() {
        return "LocalPublisherStatsImpl{"
                + "connected=" + connected
                + ", totalPublishLatency=" + totalPublishLatency
                + ", totalPublishedEventCount=" + totalPublishedEventCount
                + ", outboundQueueSize=" + outboundQueueSize
                + ", state=" + state
                + '}';
    }
}
