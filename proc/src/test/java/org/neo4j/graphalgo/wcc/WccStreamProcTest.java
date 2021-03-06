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
package org.neo4j.graphalgo.wcc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphalgo.AlgoBaseProc;
import org.neo4j.graphalgo.CommunityHelper;
import org.neo4j.graphalgo.GdsCypher;
import org.neo4j.graphalgo.NodeProjections;
import org.neo4j.graphalgo.RelationshipProjections;
import org.neo4j.graphalgo.core.CypherMapWrapper;
import org.neo4j.graphalgo.core.loading.GraphCatalog;
import org.neo4j.graphalgo.core.loading.HugeGraphFactory;
import org.neo4j.graphalgo.core.utils.paged.dss.DisjointSetStruct;
import org.neo4j.graphalgo.newapi.GraphCreateConfig;
import org.neo4j.graphalgo.newapi.ImmutableGraphCreateFromStoreConfig;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WccStreamProcTest extends WccBaseProcTest<WccStreamConfig> {

    private static final long[][] EXPECTED_COMMUNITIES = {new long[]{0L, 1L, 2L, 3L, 4, 5, 6}, new long[]{7, 8}, new long[]{9}};

    @Override
    public Class<? extends AlgoBaseProc<?, DisjointSetStruct, WccStreamConfig>> getProcedureClazz() {
        return WccStreamProc.class;
    }

    @Override
    public WccStreamConfig createConfig(CypherMapWrapper mapWrapper) {
        return WccStreamConfig.of("", Optional.empty(), Optional.empty(), mapWrapper);
    }

    @AfterEach
    void cleanCatalog() {
        GraphCatalog.removeAllLoadedGraphs();
    }

    @Test
    void testStreamWithDefaults() {
        String query = GdsCypher.call()
            .withAnyLabel()
            .withAnyRelationshipType()
            .algo("wcc")
            .streamMode()
            .yields("nodeId", "componentId");

        long [] communities = new long[10];
        runQueryWithRowConsumer(query, row -> {
            int nodeId = row.getNumber("nodeId").intValue();
            long setId = row.getNumber("componentId").longValue();
            communities[nodeId] = setId;
        });

        CommunityHelper.assertCommunities(communities, EXPECTED_COMMUNITIES);
    }

    @Test
    void testStreamRunsOnLoadedGraph() {
        GraphCreateConfig createGraphConfig = ImmutableGraphCreateFromStoreConfig
            .builder()
            .graphName("testGraph")
            .nodeProjection(NodeProjections.empty())
            .relationshipProjection(RelationshipProjections.empty())
            .build();

        GraphCatalog.set(
            createGraphConfig,
            graphLoader(createGraphConfig).build(HugeGraphFactory.class).build().graphs()
        );

        String query = GdsCypher.call()
            .explicitCreation("testGraph")
            .algo("wcc")
            .streamMode()
            .yields("nodeId", "componentId");

        long [] communities = new long[10];
        runQueryWithRowConsumer(query, row -> {
            int nodeId = row.getNumber("nodeId").intValue();
            long setId = row.getNumber("componentId").longValue();
            communities[nodeId] = setId;
        });

        CommunityHelper.assertCommunities(communities, EXPECTED_COMMUNITIES);
    }


    @Test
    void statsShouldNotHaveWriteProperties() {
        String query = GdsCypher.call()
            .withAnyLabel()
            .withAnyRelationshipType()
            .algo("wcc")
            .statsMode()
            .yields();

        runQueryWithResultConsumer(query, result -> {
            assertThat(result.columns(), not(hasItems(
                "writeMillis",
                "nodePropertiesWritten",
                "relationshipPropertiesWritten"
            )));

            if(result.hasNext()) {
                Map<String, Object> config = (Map<String, Object>) result.next().get("configuration");
                assertFalse(config.containsKey("writeProperty"));
            }
        });
    }

}
