/**
 * Copyright 2017 ClearCode Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package org.fluentd.kafka;

import influent.EventEntry;
import influent.forward.ForwardCallback;
import influent.forward.ForwardServer;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTaskContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

public class FluentdSinkTaskTest {
    private ForwardServer server;
    private final ConcurrentLinkedDeque<EventEntry> queue = new ConcurrentLinkedDeque<>();

    @Before
    public void setup() {
        ForwardCallback callback = ForwardCallback.of(stream -> {
            queue.addAll(stream.getEntries());
            return CompletableFuture.completedFuture(null);
        });
        ForwardServer.Builder builder = new ForwardServer.Builder(callback);
        server = builder.build();
        server.start();
    }

    @After
    public void teardown() {
        queue.clear();
        server.shutdown();
    }

    @Test
    public void test() throws InterruptedException {
        Map<String, String> sinkProperties = new HashMap<>();
        FluentdSinkTask task = new FluentdSinkTask();
        task.initialize(PowerMock.createMock(SinkTaskContext.class));
        //sinkProperties.put(FluentdSinkConnectorConfig.FLUENTD_CLIENT_MAX_BUFFER_BYTES, "100000");
        task.start(sinkProperties);
        final String topic = "testtopic";
        final String value = "{\"message\":\"This is a test message\"}";
        SinkRecord sinkRecord = new SinkRecord(
                topic,
                1,
                Schema.STRING_SCHEMA,
                topic,
                null,
                value,
                0,
                System.currentTimeMillis(),
                TimestampType.NO_TIMESTAMP_TYPE
        );
        task.put(Collections.singleton(sinkRecord));
        TimeUnit.SECONDS.sleep(1);
        EventEntry eventEntry = queue.poll();
        Assert.assertNotNull(eventEntry);
        Assert.assertEquals(value, eventEntry.getRecord().asMapValue().toJson());
    }
}
