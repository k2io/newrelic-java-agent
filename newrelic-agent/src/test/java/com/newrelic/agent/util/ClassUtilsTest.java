/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ClassUtilsTest {

    @Test
    public void findSuperDefinition() throws NoSuchMethodException, SecurityException {
        Method method = ArrayList.class.getMethod("get", int.class);
        Method superDef = ClassUtils.findSuperDefinition(method);

        Assert.assertNotEquals(method, superDef);
        Assert.assertEquals(List.class, superDef.getDeclaringClass());
    }

    @Test
    public void findSuperDefinition_SuperInterface() throws NoSuchMethodException, SecurityException {
        Method method = SuperInterfaceTest.class.getMethod("getMaxRows");
        Method superDef = ClassUtils.findSuperDefinition(method);

        Assert.assertNotEquals(method, superDef);
    }

    @Test
    public void findSuperDefinition_NoSuper() throws NoSuchMethodException, SecurityException {
        Method method = ArrayList.class.getDeclaredMethod("ensureCapacity", int.class);
        Method superDef = ClassUtils.findSuperDefinition(method);

        Assert.assertEquals(method, superDef);
    }

    private abstract static class SuperInterfaceTest implements PreparedStatement {

        @Override
        public int getMaxRows() throws SQLException {
            return 0;
        }

    }
}
