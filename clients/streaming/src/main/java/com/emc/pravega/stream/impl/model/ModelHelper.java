/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.emc.pravega.stream.impl.model;

import com.emc.pravega.common.netty.PravegaNodeUri;
import com.emc.pravega.controller.stream.api.v1.FutureSegment;
import com.emc.pravega.controller.stream.api.v1.NodeUri;
import com.emc.pravega.controller.stream.api.v1.Position;
import com.emc.pravega.controller.stream.api.v1.ScalingPolicyType;
import com.emc.pravega.controller.stream.api.v1.SegmentId;
import com.emc.pravega.controller.stream.api.v1.StreamConfig;
import com.emc.pravega.controller.stream.api.v1.TxId;
import com.emc.pravega.controller.stream.api.v1.TxStatus;
import com.emc.pravega.stream.PositionInternal;
import com.emc.pravega.stream.ScalingPolicy;
import com.emc.pravega.stream.Segment;
import com.emc.pravega.stream.StreamConfiguration;
import com.emc.pravega.stream.Transaction;
import com.emc.pravega.stream.impl.PositionImpl;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Provides translation (encode/decode) between the Model classes and its Thrift representation.
 */
public final class ModelHelper {

    public static final UUID encode(TxId txId) {
        Preconditions.checkNotNull(txId, "txId");
        return new UUID(txId.getHighBits(), txId.getLowBits());
    }

    public static final Segment encode(final SegmentId segment) {
        Preconditions.checkNotNull(segment, "Segment");
        return new Segment(segment.getScope(), segment.getStreamName(), segment.getNumber());
    }

    public static final com.emc.pravega.stream.impl.FutureSegment encode(final SegmentId segment, int previous) {
        Preconditions.checkNotNull(segment, "Segment");
        return new com.emc.pravega.stream.impl.FutureSegment(segment.getScope(), segment.getStreamName(), segment.getNumber(), previous);
    }

    public static final ScalingPolicy encode(final com.emc.pravega.controller.stream.api.v1.ScalingPolicy policy) {
        Preconditions.checkNotNull(policy, "ScalingPolicy");
        return new ScalingPolicy(ScalingPolicy.Type.valueOf(policy.getType().name()), policy.getTargetRate(), policy.getScaleFactor(),
                policy.getMinNumSegments());
    }

    public static final StreamConfiguration encode(final StreamConfig config) {
        Preconditions.checkNotNull(config, "StreamConfig");
        return new StreamConfiguration() {
            @Override
            public String getScope() {
                return config.getScope();
            }

            @Override
            public String getName() {
                return config.getName();
            }

            @Override
            public ScalingPolicy getScalingPolicy() {
                return encode(config.getPolicy());
            }
        };
    }

    public static final PositionImpl encode(final Position position) {
        Preconditions.checkNotNull(position, "Position");
        return new PositionImpl(encodeSegmentMap(position.getOwnedSegments()), encodeFutureSegmentMap(position.getFutureOwnedSegments()));
    }

    public static com.emc.pravega.common.netty.PravegaNodeUri encode(NodeUri uri) {
        return new com.emc.pravega.common.netty.PravegaNodeUri(uri.getEndpoint(), uri.getPort());
    }

    public static Transaction.Status encode(TxStatus status, String logString) {
        switch (status) {
            case COMMITTED:
                return Transaction.Status.COMMITTED;
            case DROPPED:
                return Transaction.Status.DROPPED;
            case OPEN:
                return Transaction.Status.OPEN;
            case SEALED:
                return Transaction.Status.SEALED;
            case UNKNOWN:
                throw new RuntimeException("Unknown transaction: " + logString);
            default:
                throw new IllegalStateException("Unknown status: " + status);
        }
    }

    public static final TxId decode(UUID txId) {
        Preconditions.checkNotNull(txId, "txId");
        return new TxId(txId.getMostSignificantBits(), txId.getLeastSignificantBits());
    }

    public static final SegmentId decode(final Segment segment) {
        Preconditions.checkNotNull(segment, "Segment");
        return new SegmentId().setScope(segment.getScope()).setStreamName(segment.getStreamName())
                              .setNumber(segment.getSegmentNumber());
    }

    public static final com.emc.pravega.controller.stream.api.v1.ScalingPolicy decode(final ScalingPolicy policyModel) {
        Preconditions.checkNotNull(policyModel, "Policy");
        return new com.emc.pravega.controller.stream.api.v1.ScalingPolicy()
                .setType(ScalingPolicyType.valueOf(policyModel.getType().name())).setTargetRate(policyModel.getTargetRate())
                .setScaleFactor(policyModel.getScaleFactor()).setMinNumSegments(policyModel.getMinNumSegments());
    }

    public static final StreamConfig decode(final StreamConfiguration configModel) {
        Preconditions.checkNotNull(configModel, "StreamConfiguration");
        return new StreamConfig(configModel.getScope(), configModel.getName(), decode(configModel.getScalingPolicy()));
    }

    public static final Position decode(final PositionInternal position) {
        Preconditions.checkNotNull(position, "Position");
        return new Position(decodeSegmentMap(position.getOwnedSegmentsWithOffsets()),
                decodeFutureSegmentMap(position.getFutureOwnedSegmentsWithOffsets()));
    }

    public static NodeUri decode(PravegaNodeUri uri) {
        return new NodeUri(uri.getEndpoint(), uri.getPort());
    }

    private static Map<Segment, Long> encodeSegmentMap(final Map<SegmentId, Long> map) {
        Preconditions.checkNotNull(map);
        HashMap<Segment, Long> result = new HashMap<>();
        for (Entry<SegmentId, Long> entry : map.entrySet()) {
            result.put(encode(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static Map<com.emc.pravega.stream.impl.FutureSegment, Long> encodeFutureSegmentMap(final Map<FutureSegment, Long> map) {
        Preconditions.checkNotNull(map);
        HashMap<com.emc.pravega.stream.impl.FutureSegment, Long> result = new HashMap<>();
        for (Entry<FutureSegment, Long> entry : map.entrySet()) {
            result.put(encode(entry.getKey().getFutureSegment(), entry.getKey().getPrecedingSegment().getNumber()), entry.getValue());
        }
        return result;
    }

    private static Map<FutureSegment, Long> decodeFutureSegmentMap(final Map<com.emc.pravega.stream.impl.FutureSegment, Long> map) {
        Preconditions.checkNotNull(map);
        HashMap<FutureSegment, Long> result = new HashMap<>();
        for (Entry<com.emc.pravega.stream.impl.FutureSegment, Long> entry : map.entrySet()) {
            String scope = entry.getKey().getScope();
            String streamName = entry.getKey().getStreamName();
            int newNumber = entry.getKey().getSegmentNumber();
            int oldNumber = entry.getKey().getPrecedingNumber();
            result.put(new FutureSegment(new SegmentId(scope, streamName, newNumber), new SegmentId(scope, streamName, oldNumber)), entry.getValue());
        }
        return result;
    }

    private static Map<SegmentId, Long> decodeSegmentMap(final Map<Segment, Long> map) {
        Preconditions.checkNotNull(map);
        return map.entrySet().stream().collect(Collectors.toMap(e -> decode(e.getKey()), Map.Entry::getValue));
    }
}