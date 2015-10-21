package org.springframework.boot.actuate.metrics.ambari.buffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;

public class MetricBufferTest {

    private MetricBuffer metricBuffer;

    private static final String M1 = "M1";
    private static final String M2 = "M2";

    @Before
    public void before() {
        metricBuffer = new MetricBuffer();
    }

    @Test
    public void testAdd() {
        assertEquals(0L, metricBuffer.size());

        metricBuffer.add(metric(M1, 11, 111));
        assertThat(metricBuffer.size(), is(1L));

        metricBuffer.add(metric(M1, 22, 222));
        assertThat(metricBuffer.size(), is(2L));
    }

    @Test
    public void testFlush() {
        metricBuffer.add(metric(M1, 11, 111));
        metricBuffer.add(metric(M1, 22, 222));
        metricBuffer.add(metric(M2, 33, 333));

        assertThat(metricBuffer.size(), is(3L));

        Map<String, Map<Long, Float>> snapshot = metricBuffer.flush();
        assertThat(metricBuffer.size(), is(0L));

        assertThat(snapshot.keySet(), hasSize(2));
        assertThat(snapshot.keySet(), containsInAnyOrder(M1, M2));

        assertThat(snapshot.get(M1).keySet(), hasSize(2));
        assertThat(snapshot.get(M1).keySet(), containsInAnyOrder(11l, 22l));
        assertThat(snapshot.get(M1).values(), containsInAnyOrder(111f, 222f));

        assertThat(snapshot.get(M2).keySet(), hasSize(1));
        assertThat(snapshot.get(M2).keySet(), containsInAnyOrder(33l));
        assertThat(snapshot.get(M2).values(), containsInAnyOrder(333f));
    }

    @Test
    public void testClose() throws IOException {
        metricBuffer.add(metric(M1, 11, 111));
        metricBuffer.add(metric(M1, 22, 222));
        metricBuffer.add(metric(M2, 33, 333));

        assertThat(metricBuffer.size(), is(3L));

        metricBuffer.close();

        assertThat(metricBuffer.size(), is(0L));
    }

    private Metric<Float> metric(String name, long timestamp, float value) {
        return new Metric<Float>(name, value, new Date(timestamp));
    }
}
