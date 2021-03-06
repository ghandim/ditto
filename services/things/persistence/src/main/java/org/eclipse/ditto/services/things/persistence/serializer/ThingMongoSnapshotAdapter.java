/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.services.things.persistence.serializer;

import javax.annotation.concurrent.ThreadSafe;

import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.model.things.Thing;
import org.eclipse.ditto.model.things.ThingsModelFactory;
import org.eclipse.ditto.services.utils.persistence.mongo.AbstractMongoSnapshotAdapter;
import org.slf4j.LoggerFactory;

/**
 * A {@link org.eclipse.ditto.services.utils.persistence.SnapshotAdapter} for snapshotting a
 * {@link org.eclipse.ditto.model.things.Thing}.
 */
@ThreadSafe
public final class ThingMongoSnapshotAdapter extends AbstractMongoSnapshotAdapter<Thing> {

    /**
     * Constructs a new {@code ThingMongoSnapshotAdapter}.
     */
    public ThingMongoSnapshotAdapter() {
        super(LoggerFactory.getLogger(ThingMongoSnapshotAdapter.class));
    }

    @Override
    protected Thing createJsonifiableFrom(final JsonObject jsonObject) {
        return ThingsModelFactory.newThing(jsonObject);
    }

}
