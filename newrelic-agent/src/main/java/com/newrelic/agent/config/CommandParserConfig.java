/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Set;

public interface CommandParserConfig {
    boolean isEnabled();

    Set<String> getDisallowedCommands();
}
