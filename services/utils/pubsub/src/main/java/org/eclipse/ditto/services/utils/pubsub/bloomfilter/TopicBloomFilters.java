/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.services.utils.pubsub.bloomfilter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.eclipse.ditto.services.utils.ddata.DistributedData;
import org.eclipse.ditto.services.utils.ddata.DistributedDataConfigReader;
import org.eclipse.ditto.services.utils.metrics.DittoMetrics;
import org.eclipse.ditto.services.utils.metrics.instruments.gauge.Gauge;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.cluster.Cluster;
import akka.cluster.ddata.Key;
import akka.cluster.ddata.LWWMap;
import akka.cluster.ddata.LWWMapKey;
import akka.cluster.ddata.Replicator;
import akka.cluster.ddata.SelfUniqueAddress;
import akka.util.ByteString;

/**
 * A distributed collection of Bloom filters of strings indexed by ActorRef.
 * The hash functions for all filter should be identical.
 */
public final class TopicBloomFilters extends DistributedData<LWWMap<ActorRef, ByteString>>
        implements TopicBloomFiltersReader, TopicBloomFiltersWriter {

    private final String topicType;
    private final SelfUniqueAddress selfUniqueAddress;

    private final Gauge topicBloomFiltersMetric = DittoMetrics.gauge("pubsub-ddata-entries");

    private TopicBloomFilters(final DistributedDataConfigReader configReader,
            final ActorRefFactory actorRefFactory,
            final ActorSystem actorSystem,
            final Executor ddataExecutor,
            final String topicType) {
        super(configReader, actorRefFactory, ddataExecutor);
        this.topicType = topicType;
        this.selfUniqueAddress = SelfUniqueAddress.apply(Cluster.get(actorSystem).selfUniqueAddress());
    }

    /**
     * Start distributed-data replicator for topic Bloom filters under an actor system's user guardian using the default
     * dispatcher.
     *
     * @param system the actor system.
     * @param ddataConfig the distributed data config.
     * @param topicType the type of messages, typically the canonical name of the message class.
     * @return access to the distributed data.
     */
    public static TopicBloomFilters of(final ActorSystem system, final DistributedDataConfigReader ddataConfig,
            final String topicType) {

        return new TopicBloomFilters(ddataConfig, system, system, system.dispatcher(), topicType);
    }

    @Override
    public CompletionStage<Collection<ActorRef>> getSubscribers(
            final Collection<? extends Collection<Integer>> topicHashes) {

        return get(Replicator.readLocal()).thenApply(optional -> {
            if (optional.isPresent()) {
                final LWWMap<ActorRef, ByteString> indexedBloomFilters = optional.get();
                topicBloomFiltersMetric.set((long) indexedBloomFilters.size());
                final Map<ActorRef, ByteString> map = indexedBloomFilters.getEntries();
                return map.entrySet()
                        .stream()
                        .filter(entry -> ByteStringAsBitSet.containsAny(entry.getValue(),
                                topicHashes.stream().map(Collection::stream)))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
            } else {
                topicBloomFiltersMetric.set(0L);
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletionStage<Void> removeAddress(final Address address,
            final Replicator.WriteConsistency writeConsistency) {
        return update(writeConsistency, lwwMap -> {
            LWWMap<ActorRef, ByteString> map = lwwMap;
            for (final ActorRef subscriber : lwwMap.getEntries().keySet()) {
                if (subscriber.path().address().equals(address)) {
                    map = map.remove(selfUniqueAddress, subscriber);
                }
            }
            return map;
        });
    }

    @Override
    public CompletionStage<Void> updateOwnTopics(final ActorRef ownSubscriber, final ByteString ownBloomFilter,
            final Replicator.WriteConsistency writeConsistency) {

        return update(writeConsistency, lwwMap -> lwwMap.put(selfUniqueAddress, ownSubscriber, ownBloomFilter));
    }

    @Override
    public CompletionStage<Void> removeSubscriber(final ActorRef subscriber,
            final Replicator.WriteConsistency writeConsistency) {
        return update(writeConsistency, lwwMap -> lwwMap.remove(selfUniqueAddress, subscriber));
    }

    @Override
    protected Key<LWWMap<ActorRef, ByteString>> getKey() {
        return LWWMapKey.create(topicType);
    }

    @Override
    protected LWWMap<ActorRef, ByteString> getInitialValue() {
        return LWWMap.empty();
    }
}
