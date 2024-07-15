/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.config;

public interface SuperAgentIntegrationConfig {
    /**
     * Check if the Super Agent integration service is enabled
     *
     * @return <code>true</code> if the Super Agent Health Check service is enabled, else <code>false</code>.
     */
    boolean isEnabled();

    /**
     * Get the domain socket listener address
     *
     * @return the domain socket address for the health check
     */
    String getHealthDeliveryLocation();

    /**
     * Return the frequency of the messages sent to the Super Agent, in seconds
     *
     * @return the health check frequency, in seconds
     */
    int getHealthReportingFrequency();
}
