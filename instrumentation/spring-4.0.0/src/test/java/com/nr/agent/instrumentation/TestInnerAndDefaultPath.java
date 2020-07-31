/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/defaultPath")
public class TestInnerAndDefaultPath {

    @RequestMapping("/innerPath")
    public String testInnerPath() {
        return "innerPath";
    }
}