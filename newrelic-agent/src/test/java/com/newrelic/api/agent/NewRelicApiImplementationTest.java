/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.ConfigConstant;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NewRelicApiImplementationTest {

    @Before
    public void setUp() {
        previousServiceManager = ServiceFactory.getServiceManager();
    }

    @After
    public void tearDown() {
        ServiceFactory.setServiceManager(previousServiceManager);
    }

    @Test
    public void noticeErrorWithExceptionShouldProduceAnExceptionReport() {
        mockOutServices();

        NewRelicApiImplementation target = new NewRelicApiImplementation();
        Exception exc = new Exception("~~ oops ~~");
        target.noticeError(exc);

        Mockito.verify(errorService).reportException(exc, Collections.emptyMap(), false);
    }

    @Test
    public void noticeErrorWithExceptionShouldProduceAnExceptionReport2() {
        mockOutServices();

        NewRelicApiImplementation target = new NewRelicApiImplementation();
        Exception exc = new Exception("~~ oops ~~");
        target.noticeError(exc, true);

        Mockito.verify(errorService).reportException(exc, Collections.emptyMap(), true);
    }

    @Test
    public void noticeErrorWithStringMessageShouldProduceAnExceptionReport() {
        mockOutServices();

        NewRelicApiImplementation target = new NewRelicApiImplementation();
        target.noticeError("errorMessage", true);

        Mockito.verify(errorService).reportError("errorMessage", Collections.emptyMap(), true);
    }

    @Test
    public void noticeErrorWithStringMessageShouldProduceAnExceptionReport2() {
        mockOutServices();

        NewRelicApiImplementation target = new NewRelicApiImplementation();
        target.noticeError("errorMessage");

        Mockito.verify(errorService).reportError("errorMessage", Collections.emptyMap(), false);
    }

    @Test
    public void noticeErrorShouldAcceptNumberAndBooleanAttributeType() {
        mockOutServices();

        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> expectedValues = new HashMap<>();

        attributes.put("MyNumber", 54);
        expectedValues.put("MyNumber", 54);

        attributes.put("MyAtomicInteger", new AtomicInteger(54));
        expectedValues.put("MyAtomicInteger", 54);

        attributes.put("MyAtomicLong", new AtomicLong(54));
        expectedValues.put("MyAtomicLong", 54L);

        attributes.put("MyAtomicBool", new AtomicBoolean(true));
        expectedValues.put("MyAtomicBool", true);

        attributes.put("MyBigDecimal", BigDecimal.valueOf(10.0000001));
        expectedValues.put("MyBigDecimal", BigDecimal.valueOf(10.0000001));

        attributes.put("MyBigInteger", BigInteger.valueOf(10000000L));
        expectedValues.put("MyBigInteger", BigInteger.valueOf(10000000L));

        // Invalid attribute values
        attributes.put("MyNaN", Double.NaN);
        attributes.put("MyPosInf", Double.POSITIVE_INFINITY);
        attributes.put("MyNegInf", Double.NEGATIVE_INFINITY);

        NewRelicApiImplementation target = new NewRelicApiImplementation();

        Exception exc = new Exception("~~ oops ~~");
        target.noticeError(exc, attributes);

        Mockito.verify(errorService).reportException(exc, expectedValues, false);

    }

    @Test
    public void noticeErrorShouldALimitAttributeCount() {
        mockOutServices();

        Map<String, Object> attributes = new HashMap<>();

        for (int i=0;i<ConfigConstant.MAX_USER_ATTRIBUTES;i++) {
            attributes.put("MyNumber"+i, i);
        }

        NewRelicApiImplementation target = new NewRelicApiImplementation();

        Exception exc = new Exception("~~ oops ~~");
        target.noticeError(exc, attributes);

        ArgumentCaptor<Map<String, ?>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(errorService).reportException(Mockito.eq(exc), mapCaptor.capture(), Mockito.eq(false));
        Assert.assertEquals(ConfigConstant.MAX_USER_ATTRIBUTES-1, mapCaptor.getValue().size()); // yes, minus 1
    }

    @Test
    public void noticeErrorShouldSerializeAttributesUsingToString() {
        mockOutServices();

        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> expectedValues = new HashMap<>();

        attributes.put("MyString", "foobar");
        expectedValues.put("MyString", "foobar");

        attributes.put("MyBoolean", true);
        expectedValues.put("MyBoolean", true);

        attributes.put("MyEnum", MyEnum.VALUE2);
        expectedValues.put("MyEnum", "VALUE2");

        NewRelicApiImplementation target = new NewRelicApiImplementation();

        Exception exc = new Exception("~~ oops ~~");
        target.noticeError(exc, attributes);

        Mockito.verify(errorService).reportException(exc, expectedValues, false);
    }

    @Test
    public void attributeSerializationHandlesExceptions() {
        mockOutServices();

        Object foo = new Object() {
            @Override
            public String toString() {
                return String.valueOf(1 / 0);
            }
        };

        Map<String, Object> attributes = new HashMap<>();
        Map<String, String> expectedValues = new HashMap<>();

        attributes.put("MyWhoops", foo);

        attributes.put("MyString", "foobar");
        expectedValues.put("MyString", "foobar");

        NewRelicApiImplementation target = new NewRelicApiImplementation();

        Exception exc = new Exception("~~ oops ~~");
        target.noticeError(exc, attributes);

        Mockito.verify(errorService).reportException(exc, expectedValues, false);
    }

    private void mockOutServices() {
        errorService = Mockito.mock(ErrorService.class);
        IRPMService rpmService = Mockito.mock(IRPMService.class);
        Mockito.when(rpmService.getErrorService()).thenReturn(errorService);

        RPMServiceManager rpmServiceManager = Mockito.mock(RPMServiceManager.class);
        Mockito.when(rpmServiceManager.getRPMService()).thenReturn(rpmService);

        MockServiceManager sm = new MockServiceManager();
        sm.setRPMServiceManager(rpmServiceManager);

        ServiceFactory.setServiceManager(sm);
    }

    private ErrorService errorService;
    private ServiceManager previousServiceManager;

    enum MyEnum {
        VALUE1,
        VALUE2,
        VALUE3
    }
}
