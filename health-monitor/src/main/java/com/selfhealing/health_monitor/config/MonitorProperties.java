package com.selfhealing.health_monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.monitor")
public class MonitorProperties {

    private String serviceAUrl;
    private String serviceAName = "service-a";
    private long intervalMs = 10000;

    public String getServiceAUrl() {
        return serviceAUrl;
    }

    public void setServiceAUrl(String serviceAUrl) {
        this.serviceAUrl = serviceAUrl;
    }

    public String getServiceAName() {
        return serviceAName;
    }

    public void setServiceAName(String serviceAName) {
        this.serviceAName = serviceAName;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }
}
