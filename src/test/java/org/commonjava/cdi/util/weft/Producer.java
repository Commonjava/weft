package org.commonjava.cdi.util.weft;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class Producer
{

    @Produces
    @ApplicationScoped
    public HealthCheckRegistry getHealthCheckRegistry()
    {
        return new HealthCheckRegistry();
    }

    @Produces
    @ApplicationScoped
    public MetricRegistry getMetricRegistry()
    {
        return new MetricRegistry();
    }
}
