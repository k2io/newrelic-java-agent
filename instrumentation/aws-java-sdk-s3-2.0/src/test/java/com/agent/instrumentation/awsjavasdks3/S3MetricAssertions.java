/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdks3;

import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.introspec.TransactionEvent;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

class S3MetricAssertions {

    static void assertMetrics(String operation) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(2000));

        String txName = introspector.getTransactionNames().iterator().next();
        assertTrue(introspector.getTransactionNames().contains(txName));

        if (operation.equals("listBuckets")) {
            // scoped
            assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "External/amazon/S3/" + operation));
            if (txName.contains("AmazonS3SyncApiTest")) {
                assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                        "Java/com.agent.instrumentation.awsjavasdks3.AmazonS3SyncApiTest/" + operation));
            } else {
                assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                        "Java/com.agent.instrumentation.awsjavasdks3.AmazonS3AsyncApiTest/" + operation));
            }

            // unscoped
            assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/amazon/S3/" + operation));
            assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
            assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));

            // events
            Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txName);
            assertEquals(1, transactionEvents.size());
            TransactionEvent transactionEvent = transactionEvents.iterator().next();
            assertEquals(1, transactionEvent.getExternalCallCount());
            assertTrue(transactionEvent.getExternalDurationInSec() > 0);

        } else {
            assertEquals(1, MetricsHelper.getScopedMetricCount(txName, "External/testbucket/S3/" + operation));
            if (txName.contains("AmazonS3SyncApiTest")) {
                assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                        "Java/com.agent.instrumentation.awsjavasdks3.AmazonS3SyncApiTest/" + operation));
            } else {
                assertEquals(1, MetricsHelper.getScopedMetricCount(txName,
                        "Java/com.agent.instrumentation.awsjavasdks3.AmazonS3AsyncApiTest/" + operation));
            }
            // unscoped
            assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/testbucket/S3/" + operation));

            // events
            Collection<TransactionEvent> transactionEvents = introspector.getTransactionEvents(txName);
            assertEquals(1, transactionEvents.size());
            TransactionEvent transactionEvent = transactionEvents.iterator().next();
            assertEquals(1, transactionEvent.getExternalCallCount());
            assertTrue(transactionEvent.getExternalDurationInSec() > 0);
        }
    }

}
