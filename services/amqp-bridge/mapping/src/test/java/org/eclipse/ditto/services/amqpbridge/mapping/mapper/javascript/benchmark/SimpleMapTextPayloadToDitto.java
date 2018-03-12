/*
 * Copyright (c) 2017 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/index.php
 *
 * Contributors:
 *    Bosch Software Innovations GmbH - initial contribution
 */
package org.eclipse.ditto.services.amqpbridge.mapping.mapper.javascript.benchmark;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.ditto.model.amqpbridge.AmqpBridgeModelFactory;
import org.eclipse.ditto.model.amqpbridge.ExternalMessage;
import org.eclipse.ditto.services.amqpbridge.mapping.mapper.MessageMapper;
import org.eclipse.ditto.services.amqpbridge.mapping.mapper.MessageMappers;
import org.eclipse.ditto.services.amqpbridge.mapping.mapper.javascript.JavaScriptMessageMapperFactory;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class SimpleMapTextPayloadToDitto implements MapToDittoProtocolScenario {

    static final String MAPPING_STRING = "A simple text to be mapped";
    private static final String CONTENT_TYPE = "text/plain";

    private static final String MAPPING_INCOMING_PLAIN =
            "function mapToDittoProtocolMsg(\n" +
                    "    headers,\n" +
                    "    textPayload,\n" +
                    "    bytePayload,\n" +
                    "    contentType\n" +
                    ") {\n" +
                    "\n" +
                    "    // ###\n" +
                    "    // Insert your mapping logic here\n" +
                    "    let namespace = \"org.eclipse.ditto\";\n" +
                    "    let id = \"jmh-test\";\n" +
                    "    let group = \"things\";\n" +
                    "    let channel = \"twin\";\n" +
                    "    let criterion = \"commands\";\n" +
                    "    let action = \"modify\";\n" +
                    "    let path = \"/attributes/foo\";\n" +
                    "    let dittoHeaders = {};\n" +
                    "    dittoHeaders[\"correlation-id\"] = headers[\"correlation-id\"];\n" +
                    "    let value = textPayload;\n" +
                    "    // ###\n" +
                    "\n" +
                    "    return buildDittoProtocolMsg(\n" +
                    "        namespace,\n" +
                    "        id,\n" +
                    "        group,\n" +
                    "        channel,\n" +
                    "        criterion,\n" +
                    "        action,\n" +
                    "        path,\n" +
                    "        dittoHeaders,\n" +
                    "        value\n" +
                    "    );\n" +
                    "}";

    private final ExternalMessage externalMessage;

    public SimpleMapTextPayloadToDitto() {
        final String correlationId = UUID.randomUUID().toString();
        final Map<String, String> headers = new HashMap<>();
        headers.put("correlation-id", correlationId);
        headers.put(ExternalMessage.CONTENT_TYPE_HEADER, CONTENT_TYPE);
        externalMessage = AmqpBridgeModelFactory.newExternalMessageBuilder(headers)
                .withText(MAPPING_STRING)
                .build();
    }

    @Override
    public MessageMapper getMessageMapper() {
        final MessageMapper javaScriptRhinoMapperPlain = MessageMappers.createJavaScriptMessageMapper();
        javaScriptRhinoMapperPlain.configureWithValidation(
                JavaScriptMessageMapperFactory
                        .createJavaScriptMessageMapperConfigurationBuilder(Collections.emptyMap())
                        .contentType(CONTENT_TYPE)
                        .incomingMappingScript(MAPPING_INCOMING_PLAIN)
                        .build()
        );
        return javaScriptRhinoMapperPlain;
    }

    @Override
    public ExternalMessage getExternalMessage() {
        return externalMessage;
    }
}

