package org.springframework.boot.actuate.metrics.ambari.pool;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetric;
import org.springframework.boot.actuate.metrics.ambari.domain.TimelineMetrics;

public class MetricObjectPoolTest {

    private MetricObjectPool metricObjectPool;

    @Before
    public void before() {
        metricObjectPool = new MetricObjectPool(10, 10);
    }

    @Test
    public void getTimelineMetrics() throws Exception {

        assertThat(borrowedeMetrics(), is(0L));
        assertThat(activeMetrics(), is(0));

        TimelineMetrics timelineMetrics = metricObjectPool.getMetrics();
        assertNotNull(timelineMetrics);

        assertThat(borrowedeMetrics(), is(1L));
        assertThat(activeMetrics(), is(1));
    }

    @Test
    public void getTimelineMetric() throws Exception {
        assertThat(borrowedeMetric(), is(0L));
        assertThat(activeMetric(), is(0));

        TimelineMetric timelineMetric = metricObjectPool.getMetricFor(metricObjectPool.getMetrics());
        assertNotNull(timelineMetric);

        assertThat(borrowedeMetric(), is(1L));
        assertThat(activeMetric(), is(1));
    }

    @Test
    public void returnTimelineMetrics() throws Exception {

        assertThat(activeMetrics(), is(0));
        assertThat(activeMetric(), is(0));

        TimelineMetrics metrics1 = metricObjectPool.getMetrics();
        metricObjectPool.getMetricFor(metrics1);
        metricObjectPool.getMetricFor(metrics1);

        TimelineMetrics metrics2 = metricObjectPool.getMetrics();
        metricObjectPool.getMetricFor(metrics2);
        metricObjectPool.getMetricFor(metrics2);

        assertThat(idleMetrics(), is(0));
        assertThat(idleMetric(), is(0));

        assertThat(activeMetrics(), is(2));
        assertThat(activeMetric(), is(4));

        metricObjectPool.returnObjects(metrics1);

        assertThat(idleMetrics(), is(1));
        assertThat(idleMetric(), is(2));

        assertThat(activeMetrics(), is(1));
        assertThat(activeMetric(), is(2));

        metricObjectPool.returnObjects(metrics2);

        assertThat(idleMetrics(), is(2));
        assertThat(idleMetric(), is(4));

        assertThat(activeMetrics(), is(0));
        assertThat(activeMetric(), is(0));

        assertThat(borrowedeMetrics(), is(2L));
        assertThat(borrowedeMetric(), is(4L));
    }

    @Test
    public void testClose() throws Exception {

        assertThat(activeMetrics(), is(0));
        assertThat(activeMetric(), is(0));

        TimelineMetrics metrics1 = metricObjectPool.getMetrics();
        metricObjectPool.getMetricFor(metrics1);
        metricObjectPool.getMetricFor(metrics1);

        TimelineMetrics metrics2 = metricObjectPool.getMetrics();
        metricObjectPool.getMetricFor(metrics2);
        metricObjectPool.getMetricFor(metrics2);

        assertThat(idleMetrics(), is(0));
        assertThat(idleMetric(), is(0));

        assertThat(activeMetrics(), is(2));
        assertThat(activeMetric(), is(4));

        metricObjectPool.close();

        assertThat(idleMetrics(), is(0));
        assertThat(idleMetric(), is(0));

        assertThat(activeMetrics(), is(2));
        assertThat(activeMetric(), is(4));

        assertThat(borrowedeMetrics(), is(2L));
        assertThat(borrowedeMetric(), is(4L));
    }

    private int idleMetrics() {
        return metricObjectPool.getTimelineMetricsPool().getNumIdle();
    }

    private int idleMetric() {
        return metricObjectPool.getTimelineMetricPool().getNumIdle();
    }

    private int activeMetrics() {
        return metricObjectPool.getTimelineMetricsPool().getNumActive();
    }

    private int activeMetric() {
        return metricObjectPool.getTimelineMetricPool().getNumActive();
    }

    private long borrowedeMetrics() {
        return metricObjectPool.getTimelineMetricsPool().getBorrowedCount();
    }

    private long borrowedeMetric() {
        return metricObjectPool.getTimelineMetricPool().getBorrowedCount();
    }

}
