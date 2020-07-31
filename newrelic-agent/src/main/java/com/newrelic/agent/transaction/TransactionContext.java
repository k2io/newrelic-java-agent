/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;


public interface TransactionContext {

    void _nr_setTransaction(Object tx);

    Object _nr_getTransaction();

}