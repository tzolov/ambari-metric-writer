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
    private static final String M3 = "M3";

    private static final long TS1 = 11;
    private static final long TS2 = 22;
    private static final long TS3 = 33;

    @Before
    public void before() {
        metricBuffer = new MetricBuffer();
    }

    @Test
    public void testAdd() {
        assertEquals(0L, metricBuffer.size());

        metricBuffer.add(metric1(M1, TS1, 111.0));
        assertThat(metricBuffer.size(), is(1L));

        metricBuffer.add(metric1(M1, TS2, 222.0));
        assertThat(metricBuffer.size(), is(2L));
    }

    @Test
    public void testFlush() {
        metricBuffer.add(metric1(M1, TS1, 111.0));
        metricBuffer.add(metric1(M1, TS2, 222.0));
        metricBuffer.add(metric1(M2, TS3, 333.0));

        assertThat(metricBuffer.size(), is(3L));

        Map<String, Map<Long, Double>> snapshot = metricBuffer.flush();
        assertThat(metricBuffer.size(), is(0L));

        assertThat(snapshot.keySet(), hasSize(2));
        assertThat(snapshot.keySet(), containsInAnyOrder(M1, M2));

        assertThat(snapshot.get(M1).keySet(), hasSize(2));
        assertThat(snapshot.get(M1).keySet(), containsInAnyOrder(TS1, TS2));
        assertThat(snapshot.get(M1).values(), containsInAnyOrder(111.0, 222.0));

        assertThat(snapshot.get(M2).keySet(), hasSize(1));
        assertThat(snapshot.get(M2).keySet(), containsInAnyOrder(TS3));
        assertThat(snapshot.get(M2).values(), containsInAnyOrder(333.0));
    }

    @Test
    public void testClose() throws IOException {
        metricBuffer.add(metric1(M1, TS1, 111.0));
        metricBuffer.add(metric1(M1, TS2, 222.0));
        metricBuffer.add(metric1(M2, TS3, 333.0));

        assertThat(metricBuffer.size(), is(3L));

        metricBuffer.close();

        assertThat(metricBuffer.size(), is(0L));
    }

    @Test
    public void metricTypes() {
        metricBuffer.add(new Metric<Double>(M1, 11.0, new Date(TS1)));
        assertThat(metricBuffer.getMetricType(M1), is("Double"));

        metricBuffer.add(new Metric<Long>(M2, 11L, new Date(TS2)));
        assertThat(metricBuffer.getMetricType(M2), is("Long"));

        metricBuffer.add(new Metric<Float>(M3, 11.0f, new Date(TS3)));
        assertThat(metricBuffer.getMetricType(M3), is("Float"));
    }

    private Metric<?> metric1(String name, long timestamp, Double value) {
        return new Metric<Double>(name, value, new Date(timestamp));
    }
}
