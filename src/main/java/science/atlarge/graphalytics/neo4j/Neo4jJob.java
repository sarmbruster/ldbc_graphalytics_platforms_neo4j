/*
 * Copyright 2015 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package science.atlarge.graphalytics.neo4j;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;
import science.atlarge.graphalytics.execution.PlatformExecutionException;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Base class for all jobs in the platform driver. Configures and executes a platform job using the parameters
 * and executable specified by the subclass for a specific algorithm.
 *
 * @author Stefan Armbruster
 */
public class Neo4jJob {

	private static final Logger LOG = LogManager.getLogger();

	private final String graphName;

	private final Neo4jConfiguration platformConfig;
	private final String nodeProjection;
	private final String relationshipProjection;
	private final Map<String,Object> options;
	private final String cypherAlgoCall;
	private final String outputPath;
	private final Map<String, Object> config;

	public Neo4jJob(Neo4jConfiguration platformConfig,
					String nodeProjection, String relationshipProjection, Map<String,Object> options,
					String cypherAlgoCall, String graphName, Map<String,Object> config, String outputPath) {
		this.platformConfig = platformConfig;
        this.nodeProjection = nodeProjection;
		this.relationshipProjection = relationshipProjection;
		this.options = options;
		this.cypherAlgoCall = cypherAlgoCall;
		this.graphName = graphName;
		this.config = config;
		this.outputPath = outputPath;
    }
	public void execute() throws PlatformExecutionException {

		try (Driver driver = GraphDatabase.driver("neo4j://localhost");
			 Session session = driver.session(SessionConfig.forDatabase(graphName))) {
			session.executeWrite(t -> {
				runAndLog(t, "CALL gds.graph.project($graphName, $nodeProjection, $relationshipProjection, $options)",
						Map.of(
								"graphName", graphName,
								"nodeProjection", nodeProjection,
								"relationshipProjection", relationshipProjection,
								"options", options
						)
				);

				ProcTimeLog.start();
				runAndLog(t, cypherAlgoCall, Map.of("graphName", graphName, "config", config));
				ProcTimeLog.end();

				try (FileWriter writer = new FileWriter(outputPath)) {
					List<Record> result = t.run(String.format("""
							CALL gds.graph.nodeProperties.stream('%s','pagerank') yield nodeId, propertyValue
							return gds.util.asNode(nodeId).VID as id, propertyValue
							""", graphName)).list();
					// NOTE: neo4j does not normalize pagerank (and maybe others)
					double total = result.stream().mapToDouble(record -> record.get("propertyValue").asDouble()).sum();
					for (Record record: result) {
						writer.write( String.format(Locale.US, "%d %f\n", record.get("id").asLong(),
								record.get("propertyValue").asDouble() / total ));
					}

					runAndLog(t, "CALL gds.graph.drop($graphName)", Map.of("graphName", graphName));
					return null;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (Exception e) {
			throw new PlatformExecutionException("Failed to execute a Neo4j job.", e);
		}
	}

	private Result runAndLog(TransactionContext t, String cypher, Map<String, Object> params) {
		LOG.info("running {} with params {}", cypher, params);
		return t.run(cypher, params);
	}

	private Result runAndLog(TransactionContext t, String cypher) {
		return runAndLog(t, cypher, Collections.emptyMap());
	}

}
