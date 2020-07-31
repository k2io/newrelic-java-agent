/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.bridge.external.ExternalParameters;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This tracer is the simplest tracer that money can buy. It currently only tracks duration of the method,
 * and does not allow creation of children.
 * It is akin to the bytecode generated by the FlyweightTraceMethodVisitor in the Weaver.
 *
 * <p>
 * Also note: this tracer will never be included in transaction traces.
 *
 * @see com.newrelic.agent.instrumentation.tracing.FlyweightTraceMethodVisitor
 */
public class UltraLightTracer implements Tracer {

    private final TransactionActivity txa;
    private final ClassMethodSignature classMethodSignature;
    private final Tracer parentTracer;
    private final long startNanos = System.nanoTime();
    private final AtomicLong endNanos = new AtomicLong();
    private final String segmentName;

    public static UltraLightTracer createClampedSegment(TransactionActivity txa, ClassMethodSignature classMethodSignature) {
        return new UltraLightTracer(txa, classMethodSignature, "Clamped");
    }

    private UltraLightTracer(TransactionActivity txa, ClassMethodSignature classMethodSignature, final String metricPrefix) {
        this.txa = txa;
        this.classMethodSignature = classMethodSignature;
        this.parentTracer = txa.getLastTracer();
        this.segmentName = metricPrefix + "/" + classMethodSignature.getClassName() + "/" + classMethodSignature.getMethodName();
        txa.tracerStarted(this);
    }

    @Override
    public void finish(int opcode, Object returnValue) {
        doFinish(opcode);
    }

    @Override
    public void finish(Throwable throwable) {
        doFinish(Opcodes.ATHROW);
    }

    private void doFinish(int opcode) {
        endNanos.set(System.nanoTime());
        //this call makes sure that this tracer is popped off the stack
        txa.tracerFinished(this, opcode);
        //record the stats
        txa.getTransactionStats().getScopedStats().getOrCreateResponseTimeStats(segmentName).recordResponseTimeInNanos(getDuration());
        //make sure to let the parent know we're done
        parentTracer.childTracerFinished(this);
    }

    @Override
    public boolean isMetricProducer() {
        return true;
    }

    @Override
    public TransactionActivity getTransactionActivity() {
        return txa;
    }

    @Override
    public Tracer getParentTracer() {
        return parentTracer;
    }

    @Override
    public TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator, long startTime,
            TransactionSegment lastSibling) {
        return new TransactionSegment(ttConfig, sqlObfuscator, startTime, this);
    }

    @Override
    public String getTransactionSegmentName() {
        return segmentName;
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getCustomAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public ClassMethodSignature getClassMethodSignature() {
        return classMethodSignature;
    }

    @Override
    public boolean isTransactionSegment() {
        return false;
    }

    @Override
    public long getStartTimeInMillis() {
        return getStartTimeInMilliseconds();
    }

    @Override
    public long getStartTimeInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(startNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public long getEndTimeInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(endNanos.get(), TimeUnit.NANOSECONDS);
    }

    @Override
    public long getExclusiveDuration() {
        return getDuration();
    }

    @Override
    public long getDuration() {
        return endNanos.get() - startNanos;
    }

    @Override
    public String getMetricName() {
        return segmentName;
    }

    /////////////////////////////////////////////
    //// No-ops below here
    /////////////////////////////////////////////

    @Override
    public void addCustomAttribute(String key, Number value) {
    }

    @Override
    public void addCustomAttribute(String key, String value) {
    }

    @Override
    public void addCustomAttribute(String key, boolean value) {
    }

    @Override
    public void addCustomAttributes(Map<String, Object> attributes) {
    }

    @Override
    public long getStartTime() {
        return 0;
    }

    @Override
    public long getEndTime() {
        return 0;
    }

    @Override
    public long getDurationInMilliseconds() {
        return 0;
    }

    @Override
    public long getRunningDurationInNanos() {
        return 0;
    }

    @Override
    public TracedMethod getParentTracedMethod() {
        //note: all current callers do null-checks on this, so it should be safe.
        return null;
    }

    @Override
    public void setRollupMetricNames(String... metricNames) {
    }

    @Override
    public void setMetricNameFormatInfo(String metricName, String transactionSegmentName, String transactionSegmentUri) {
    }

    @Override
    public void addExclusiveRollupMetricName(String... metricNameParts) {
    }

    @Override
    public void nameTransaction(TransactionNamePriority namePriority) {
    }

    @Override
    public void setCustomMetricPrefix(String prefix) {
    }

    @Override
    public void setTrackChildThreads(boolean shouldTrack) {
    }

    @Override
    public boolean trackChildThreads() {
        return false;
    }

    @Override
    public String getTransactionSegmentUri() {
        return null;
    }

    @Override
    public void setAgentAttribute(String key, Object value) {
    }

    @Override
    public void removeAgentAttribute(String key) {
    }

    @Override
    public Object getAgentAttribute(String key) {
        return null;
    }

    @Override
    public void childTracerFinished(Tracer child) {
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public void setParentTracer(Tracer tracer) {
    }

    @Override
    public boolean isParent() {
        return false;
    }

    @Override
    public boolean isChildHasStackTrace() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void removeTransactionSegment() {
        // no-op because this is never a transaction segment.
    }

    @Override
    public void markFinishTime() {
    }

    @Override
    public String getGuid() {
        return null;
    }

    @Override
    public com.newrelic.api.agent.ExternalParameters getExternalParameters() {
        return null;
    }

    @Override
    public void setNoticedError(Throwable throwable) {
    }

    @Override
    public Throwable getException() {
        return null;
    }

    @Override
    public void setThrownException(Throwable throwable) {
    }

    @Override
    public boolean wasExceptionSetByAPI() {
        return false;
    }

    @Override
    public void setMetricName(String... metricNameParts) {
    }

    @Override
    public void addRollupMetricName(String... metricNameParts) {
    }

    @Override
    public void reportAsExternal(com.newrelic.api.agent.ExternalParameters externalParameters) {
    }

    @Override
    public void addOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
    }

    @Override
    public void readInboundResponseHeaders(InboundHeaders inboundResponseHeaders) {
    }

    @Override
    public void reportAsExternal(ExternalParameters externalParameters) {
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return null;
    }
}
