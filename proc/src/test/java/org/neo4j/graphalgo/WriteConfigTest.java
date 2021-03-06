/*
 * Copyright (c) 2017-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.graphalgo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.core.CypherMapWrapper;
import org.neo4j.graphalgo.newapi.AlgoBaseConfig;
import org.neo4j.graphalgo.newapi.WriteConfig;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.MapUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public interface WriteConfigTest<CONFIG extends WriteConfig & AlgoBaseConfig, RESULT> extends AlgoBaseProcTest<CONFIG, RESULT> {

    @Test
    default void testMissingWritePropertyFails() {
        CypherMapWrapper mapWrapper =
            createMinimalConfig(CypherMapWrapper.empty())
                .withoutEntry("writeProperty");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> createConfig(mapWrapper)
        );
        assertEquals(
            "No value specified for the mandatory configuration parameter `writeProperty`",
            exception.getMessage()
        );
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.graphalgo.AlgoBaseProcTest#emptyStringPropertyValues")
    default void testEmptyWritePropertyValues(String writePropertyParameter) {
        CypherMapWrapper mapWrapper = CypherMapWrapper.create(MapUtil.map("writeProperty", writePropertyParameter));
        assertThrows(IllegalArgumentException.class, () -> createConfig(mapWrapper));
    }

    @Test
    default void testWriteConfig() {
        CypherMapWrapper mapWrapper = CypherMapWrapper.create(MapUtil.map(
            "writeProperty", "writeProperty",
            "writeConcurrency", 42
        ));
        CONFIG config = createConfig(createMinimalConfig(mapWrapper));
        assertEquals("writeProperty", config.writeProperty());
        assertEquals(42, config.writeConcurrency());
    }

    default void checkMillisSet(Result.ResultRow row) {
        assertTrue(row.getNumber("createMillis").intValue() >= 0, "load time not set");
        assertTrue(row.getNumber("computeMillis").intValue() >= 0, "compute time not set");
        assertTrue(row.getNumber("writeMillis").intValue() >= 0, "write time not set");
    }
}
