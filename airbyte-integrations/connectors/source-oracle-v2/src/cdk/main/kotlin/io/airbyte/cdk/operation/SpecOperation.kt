/*
 * Copyright (c) 2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.cdk.operation

import io.airbyte.cdk.command.ConfigurationJsonObjectSupplier
import io.airbyte.cdk.consumers.OutputConsumer
import io.airbyte.protocol.models.v0.ConnectorSpecification
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import java.net.URI

@Singleton
@Requires(property = CONNECTOR_OPERATION, value = "spec")
class SpecOperation(
    @Value("\${airbyte.connector.documentationUrl}") val documentationUrl: String,
    val configJsonObjectSupplier: ConfigurationJsonObjectSupplier<*>,
    val outputConsumer: OutputConsumer
) : Operation {

    override val type = OperationType.SPEC

    override fun execute() {
        outputConsumer.accept(
            ConnectorSpecification()
                .withDocumentationUrl(URI.create(documentationUrl))
                .withConnectionSpecification(configJsonObjectSupplier.jsonSchema)
        )
    }
}
