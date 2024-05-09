package io.airbyte.cdk.discover

import com.fasterxml.jackson.databind.node.ObjectNode
import io.airbyte.cdk.jdbc.ColumnMetadata
import io.airbyte.protocol.models.v0.AirbyteStream
import io.airbyte.protocol.models.v0.SyncMode
import io.micronaut.context.annotation.DefaultImplementation
import jakarta.inject.Singleton

@DefaultImplementation(DefaultAirbyteStreamDecorator::class)
interface AirbyteStreamDecorator {

    /** Connector-specific [AirbyteStream] decoration logic for GLOBAL-state streams. */
    fun decorateGlobal(airbyteStream: AirbyteStream)

    /** Connector-specific [AirbyteStream] decoration logic for STREAM-state streams. */
    fun decorateNonGlobal(airbyteStream: AirbyteStream)

    /**
     * Can the column be used as part of a primary key in a resumable initial sync?
     *
     * For this to be possible,
     * 1. the column needs to be part of a key as defined by the source relation,
     * 2. and its values must be settable as parameters in a [java.sql.PreparedStatement].
     *
     * This method does not determine (1), of course, because the source relation keys are defined
     * in the source database itself and are retrieved via [MetadataQuerier.primaryKeys]. Instead,
     * this method determines (2) based on the type information of the column, typically the
     * [FieldType] objects. For instance if the [ColumnMetadata] does not map to a
     * [ReversibleFieldType] then the column can't reliably round-trip checkpoint values during a
     * resumable initial sync.
     */
    fun isPossiblePrimaryKeyElement(field: Field): Boolean

    /**
     * Can the column be used as a cursor in a cursor-based incremental sync?
     *
     * This predicate is like [isPossiblePrimaryKeyElement] but tighter: in addition to being
     * able to round-trip the column values, we need to be able to aggregate them using the MAX()
     * SQL function.
     */
    fun isPossibleCursor(field: Field): Boolean
}

@Singleton
class DefaultAirbyteStreamDecorator : AirbyteStreamDecorator {

    override fun decorateGlobal(airbyteStream: AirbyteStream) {
        airbyteStream.apply {
            supportedSyncModes = listOf(SyncMode.FULL_REFRESH, SyncMode.INCREMENTAL)
            (jsonSchema["properties"] as ObjectNode).apply {
                for (metaField in listOf(AirbyteCdcLsnMetaField, AirbyteCdcUpdatedAtMetaField, AirbyteCdcDeletedAtMetaField)) {
                    set<ObjectNode>(metaField.id, metaField.type.airbyteType.asJsonSchema())
                }
            }
            defaultCursorField = listOf(AirbyteCdcLsnMetaField.id)
            sourceDefinedCursor = true
        }
    }

    override fun decorateNonGlobal(airbyteStream: AirbyteStream) {
        airbyteStream.apply {
            supportedSyncModes =
                if (defaultCursorField.isEmpty()) {
                    listOf(SyncMode.FULL_REFRESH)
                } else {
                    listOf(SyncMode.FULL_REFRESH, SyncMode.INCREMENTAL)
                }
        }
    }

    override fun isPossiblePrimaryKeyElement(field: Field): Boolean =
        when (field.type) {
            !is ReversibleFieldType -> false
            BinaryStreamFieldType,
            CharacterStreamFieldType,
            NCharacterStreamFieldType,
            ClobFieldType,
            NClobFieldType,
            JsonStringFieldType -> false
            else -> true
        }

    override fun isPossibleCursor(field: Field): Boolean =
        isPossiblePrimaryKeyElement(field) && field.type !is BooleanFieldType
}
