/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.CrossProcessConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InboundHeaderStateTest {

    private final MockServiceManager serviceManager = new MockServiceManager();
    private final String encodingKey = getClass().getName();
    private InboundHeaderState ihs;
    private ExtendedRequest request;
    private CrossProcessConfig config;
    private Transaction tx;

    @Before
    public void setup() {

        serviceManager.setConfigService(new MockConfigService(AgentConfigFactory.createAgentConfig(
                Collections.<String, Object> emptyMap(), Collections.<String, Object> emptyMap(), null)));
        ServiceFactory.setServiceManager(serviceManager);

        OutboundHeaders outboundHeaders = mock(OutboundHeaders.class);
        InboundHeaders inboundHeaders = mock(InboundHeaders.class);
        request = mock(ExtendedRequest.class);
        config = mock(CrossProcessConfig.class);
        tx = mock(Transaction.class);

        when(inboundHeaders.getHeaderType()).thenReturn(HeaderType.HTTP);
        when(outboundHeaders.getHeaderType()).thenReturn(HeaderType.HTTP);
        when(config.getEncodingKey()).thenReturn(encodingKey);
        when(config.isCrossApplicationTracing()).thenReturn(true);
        when(tx.isIgnore()).thenReturn(false);
        when(tx.getDispatcher()).thenReturn(mock(Dispatcher.class));
        when(tx.getDispatcher().getRequest()).thenReturn(request);
        when(tx.getDispatcher().getRequest().getHeaderType()).thenReturn(HeaderType.HTTP);
        when(tx.getCrossProcessConfig()).thenReturn(config);
        when(tx.getPriorityTransactionName()).thenReturn(
                PriorityTransactionName.create("Test", "TEST", TransactionNamePriority.NONE));
        when(tx.getApplicationName()).thenReturn("TestApp");
        when(tx.getLock()).thenReturn(new Object());
        when(tx.getAgentConfig()).thenReturn(ServiceFactory.getConfigService().getDefaultAgentConfig());
    }

    @Test
    public void testNullHeaders() throws UnsupportedEncodingException {
        ihs = new InboundHeaderState(tx, null);
        assertNull(ihs.getClientCrossProcessId());
        assertNull(ihs.getInboundTripId());
        assertNull(ihs.getReferrerGuid());
        assertNull(ihs.getReferringPathHash());
        assertEquals(-1L, ihs.getRequestContentLength());
    }

    @Test
    public void testNoNRHeaders() {
        when(request.getHeader("X-Foo")).thenReturn("FOO");
        when(request.getHeader("X-Bar")).thenReturn("BAR");
        when(request.getHeader("X-Baz")).thenReturn("BAZ");
        when(request.getHeader("Content-Length")).thenReturn("42");

        assertTrue(config.isCrossApplicationTracing());

        ihs = new InboundHeaderState(tx, request);
        assertNull(ihs.getClientCrossProcessId());
        assertNull(ihs.getInboundTripId());
        assertNull(ihs.getReferrerGuid());
        assertNull(ihs.getReferringPathHash());
        assertEquals(42L, ihs.getRequestContentLength());
    }

    @Test
    public void testBadNRHeaders1() {
        when(request.getHeader("X-Foo")).thenReturn("FOO");
        when(request.getHeader("X-Bar")).thenReturn("BAR");
        when(request.getHeader("X-Baz")).thenReturn("BAZ");

        when(request.getHeader(HeadersUtil.NEWRELIC_APP_DATA_HEADER)).thenReturn("FOO");
        // when(request.getHeader(HeadersUtil.NEWRELIC_ID_HEADER)).thenReturn("BAR");
        // when(request.getHeader(HeadersUtil.NEWRELIC_TRANSACTION_HEADER)).thenReturn("BAZ");
        // when(request.getHeader("Content-Length")).thenReturn("42");

        assertTrue(config.isCrossApplicationTracing());

        ihs = new InboundHeaderState(tx, request);
        assertNull(ihs.getClientCrossProcessId());
        assertNull(ihs.getInboundTripId());
        assertNull(ihs.getReferrerGuid());
        assertNull(ihs.getReferringPathHash());
        assertEquals(-1L, ihs.getRequestContentLength());
    }

    @Test
    public void testBadNRHeaders2() {
        when(request.getHeader("X-Foo")).thenReturn("FOO");
        when(request.getHeader("X-Bar")).thenReturn("BAR");
        when(request.getHeader("X-Baz")).thenReturn("BAZ");

        // when(request.getHeader(HeadersUtil.NEWRELIC_APP_DATA_HEADER)).thenReturn("FOO");
        when(request.getHeader(HeadersUtil.NEWRELIC_ID_HEADER)).thenReturn("BAR");
        // when(request.getHeader(HeadersUtil.NEWRELIC_TRANSACTION_HEADER)).thenReturn("BAZ");
        // when(request.getHeader("Content-Length")).thenReturn("42");

        assertTrue(config.isCrossApplicationTracing());

        ihs = new InboundHeaderState(tx, request);
        assertNull(ihs.getClientCrossProcessId());
        assertNull(ihs.getInboundTripId());
        assertNull(ihs.getReferrerGuid());
        assertNull(ihs.getReferringPathHash());
        assertEquals(-1L, ihs.getRequestContentLength());
    }

    @Test
    public void testBadNRHeaders3() {
        when(request.getHeader("X-Foo")).thenReturn("FOO");
        when(request.getHeader("X-Bar")).thenReturn("BAR");
        when(request.getHeader("X-Baz")).thenReturn("BAZ");

        // when(request.getHeader(HeadersUtil.NEWRELIC_APP_DATA_HEADER)).thenReturn("FOO");
        // when(request.getHeader(HeadersUtil.NEWRELIC_ID_HEADER)).thenReturn("BAR");
        when(request.getHeader(HeadersUtil.NEWRELIC_TRANSACTION_HEADER)).thenReturn("BAZ");
        // when(request.getHeader("Content-Length")).thenReturn("42");

        assertTrue(config.isCrossApplicationTracing());

        ihs = new InboundHeaderState(tx, request);
        assertNull(ihs.getClientCrossProcessId());
        assertNull(ihs.getInboundTripId());
        assertNull(ihs.getReferrerGuid());
        assertNull(ihs.getReferringPathHash());
        assertEquals(-1L, ihs.getRequestContentLength());
    }

    @Test
    public void testBadNRHeaders4() {
        when(request.getHeader("X-Foo")).thenReturn("FOO");
        when(request.getHeader("X-Bar")).thenReturn("BAR");
        when(request.getHeader("X-Baz")).thenReturn("BAZ");

        // when(request.getHeader(HeadersUtil.NEWRELIC_APP_DATA_HEADER)).thenReturn("FOO");
        // when(request.getHeader(HeadersUtil.NEWRELIC_ID_HEADER)).thenReturn("BAR");
        // when(request.getHeader(HeadersUtil.NEWRELIC_TRANSACTION_HEADER)).thenReturn("BAZ");
        when(request.getHeader("Content-Length")).thenReturn("One would expect a number here.");

        assertTrue(config.isCrossApplicationTracing());

        ihs = new InboundHeaderState(tx, request);
        assertNull(ihs.getClientCrossProcessId());
        assertNull(ihs.getInboundTripId());
        assertNull(ihs.getReferrerGuid());
        assertNull(ihs.getReferringPathHash());
        assertEquals(-1L, ihs.getRequestContentLength());
    }

    // APP_DATA "[\"6#66\",\"WebTransaction\\/test\\/test\",1.0,0.2,12345,\"56b0d429ee4730fe\"]"
    // TRANSACTION "[\"ee8e5ef1a374c0ec\",false,\"ee8e5ef1a374c0ec\",\"8fc87490\"]";

    @Test
    public void testBadNRHeaders5() {
        when(request.getHeader("X-Foo")).thenReturn("FOO");
        when(request.getHeader("X-Bar")).thenReturn("BAR");
        when(request.getHeader("X-Baz")).thenReturn("BAZ");

        when(request.getHeader(HeadersUtil.NEWRELIC_APP_DATA_HEADER)).thenReturn(
                "[\"6#66\",\"WebTransaction\\/test\\/test\",Nan,NotAFloat,NotAnInt,\"NotAHexString\"]");
        // when(request.getHeader(HeadersUtil.NEWRELIC_ID_HEADER)).thenReturn("BAR");
        // when(request.getHeader(HeadersUtil.NEWRELIC_TRANSACTION_HEADER)).thenReturn(
        // "[\"ee8e5ef1a374c0ec\",false,\"ee8e5ef1a374c0ec\",\"8fc87490\"]");
        // when(request.getHeader("Content-Length")).thenReturn("One would expect a number here.");

        assertTrue(config.isCrossApplicationTracing());

        ihs = new InboundHeaderState(tx, request);
        assertNull(ihs.getClientCrossProcessId());
        assertNull(ihs.getInboundTripId());
        assertNull(ihs.getReferrerGuid());
        assertNull(ihs.getReferringPathHash());
        assertEquals(-1L, ihs.getRequestContentLength());
    }

    @Test
    public void testBadNRHeaders6() {
        when(request.getHeader("X-Foo")).thenReturn("FOO");
        when(request.getHeader("X-Bar")).thenReturn("BAR");
        when(request.getHeader("X-Baz")).thenReturn("BAZ");

        // when(request.getHeader(HeadersUtil.NEWRELIC_APP_DATA_HEADER)).thenReturn(
        // "[\"6#66\",\"WebTransaction\\/test\\/test\",Nan,NotAFloat,NotAnInt,\"NotAHexString\"]");
        // when(request.getHeader(HeadersUtil.NEWRELIC_ID_HEADER)).thenReturn("BAR");
        when(request.getHeader(HeadersUtil.NEWRELIC_TRANSACTION_HEADER)).thenReturn(
                "[\"ee8e5ef1a374c0ec\",NotABoolean,\"ee8eZZZZZZc0ec\",\"8fZZZZ90\"]");
        // when(request.getHeader("Content-Length")).thenReturn("One would expect a number here.");

        assertTrue(config.isCrossApplicationTracing());

        ihs = new InboundHeaderState(tx, request);
        assertNull(ihs.getClientCrossProcessId());
        assertNull(ihs.getInboundTripId());
        assertNull(ihs.getReferrerGuid());
        assertNull(ihs.getReferringPathHash());
        assertEquals(-1L, ihs.getRequestContentLength());
    }
}
