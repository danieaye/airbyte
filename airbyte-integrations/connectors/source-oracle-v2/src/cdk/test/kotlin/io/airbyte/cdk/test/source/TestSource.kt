/*
 * Copyright (c) 2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.cdk.test.source

import io.airbyte.cdk.AirbyteSourceRunner

/** A fake source database connector, vaguely compatible with the H2 database. */
class TestSource {

    fun main(args: Array<String>) {
        AirbyteSourceRunner.run(*args)
    }
}
