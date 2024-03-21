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
import science.atlarge.graphalytics.domain.algorithms.Algorithm;
import science.atlarge.graphalytics.domain.algorithms.AlgorithmParameters;
import science.atlarge.graphalytics.domain.algorithms.EmptyParameters;
import science.atlarge.graphalytics.domain.algorithms.PageRankParameters;
import science.atlarge.graphalytics.domain.algorithms.CommunityDetectionLPParameters;
import science.atlarge.graphalytics.domain.benchmark.BenchmarkRun;
import science.atlarge.graphalytics.domain.graph.FormattedGraph;
import science.atlarge.graphalytics.domain.graph.LoadedGraph;
import science.atlarge.graphalytics.execution.*;
import science.atlarge.graphalytics.report.result.BenchmarkMetrics;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

/**
 * Neo4j platform driver for the Graphalytics benchmark.
 *
 * @author Gábor Szárnyas
 * @author Bálint Hegyi
 */
public class Neo4jPlatform implements Platform {

	protected static final Logger LOG = LogManager.getLogger();
	private static final String PLATFORM_NAME = "neo4j";

	public Neo4jLoader loader;

	@Override
	public void verifySetup() { }

	@Override
	public LoadedGraph loadGraph(FormattedGraph formattedGraph) throws Exception {
		Neo4jConfiguration platformConfig = Neo4jConfiguration.parsePropertiesFile();
		loader = new Neo4jLoader(formattedGraph, platformConfig);

		LOG.info("Loading graph " + formattedGraph.getName());
		Path loadedPath = Paths.get("./intermediate").resolve(formattedGraph.getName());

		try {
			int exitCode = loader.load(loadedPath.toString());
			if (exitCode != 0) {
				throw new PlatformExecutionException("Neo4j exited with an error code: " + exitCode);
			}
		} catch (Exception e) {
			throw new PlatformExecutionException("Failed to load a Neo4j dataset.", e);
		}
		LOG.info("Loaded graph " + formattedGraph.getName());

		Path databasePath = loadedPath.resolve("database");
		return new LoadedGraph(formattedGraph, databasePath.toString());
	}

	@Override
	public void deleteGraph(LoadedGraph loadedGraph) throws Exception {
		LOG.info("Unloading graph " + loadedGraph.getFormattedGraph().getName());
		try {

			int exitCode = loader.unload(loadedGraph.getLoadedPath());
			if (exitCode != 0) {
				throw new PlatformExecutionException("Neo4j exited with an error code: " + exitCode);
			}
		} catch (Exception e) {
			throw new PlatformExecutionException("Failed to unload a Neo4j dataset.", e);
		}
		LOG.info("Unloaded graph " +  loadedGraph.getFormattedGraph().getName());
	}

	@Override
	public void prepare(RunSpecification runSpecification) { }

	@Override
	public void startup(RunSpecification runSpecification) {
		BenchmarkRunSetup benchmarkRunSetup = runSpecification.getBenchmarkRunSetup();
		Path logDir = benchmarkRunSetup.getLogDir().resolve("platform").resolve("runner.logs");
		Neo4jCollector.startPlatformLogging(logDir);
	}

	@Override
	public void run(RunSpecification runSpecification) throws PlatformExecutionException {
		BenchmarkRun benchmarkRun = runSpecification.getBenchmarkRun();
		BenchmarkRunSetup benchmarkRunSetup = runSpecification.getBenchmarkRunSetup();
//		RuntimeSetup runtimeSetup = runSpecification.getRuntimeSetup();
		String outputPath = benchmarkRunSetup.getOutputDir().resolve(benchmarkRun.getName()).toAbsolutePath().toString();


		Algorithm algorithm = benchmarkRun.getAlgorithm();
		AlgorithmParameters algorithmParameters = benchmarkRun.getAlgorithmParameters();
		Neo4jConfiguration platformConfig = Neo4jConfiguration.parsePropertiesFile();
		LOG.info("Executing benchmark with algorithm \"{}\" on graph \"{}\".",
				benchmarkRun.getAlgorithm().getName(),
				benchmarkRun.getFormattedGraph().getName());

		String graphName = benchmarkRun.getGraph().getName();
		String cypher;
		Map<String,Object> config;
		switch (algorithm) {
			case PR:
				PageRankParameters paramsPr = (PageRankParameters) algorithmParameters;
				config = Map.of("maxIterations", paramsPr.getNumberOfIterations(),
						"dampingFactor", paramsPr.getDampingFactor(),
						"mutateProperty", "result");
				cypher = "CALL gds.pageRank.mutate($graphName, $config)";

				break;
			case WCC:
				EmptyParameters paramsWCC = (EmptyParameters) algorithmParameters;
				config = Map.of(
						"mutateProperty", "result");
				cypher = "CALL gds.wcc.mutate($graphName, $config)";

				break;
			case LCC:
				EmptyParameters paramsLCC = (EmptyParameters) algorithmParameters;
				config = Map.of(
						"mutateProperty", "result");
				cypher = "CALL gds.localClusteringCoefficient.mutate($graphName, $config)";

				break;
			case CDLP:
				CommunityDetectionLPParameters paramsCDLP = (CommunityDetectionLPParameters) algorithmParameters;
				config = Map.of("maxIterations", paramsCDLP.getMaxIterations(),
						"mutateProperty", "result");
				cypher = "CALL gds.labelPropagation.mutate($graphName, $config)";

				break;
			default:
				throw new IllegalArgumentException("Neo4j Platform Driver doesn't how to run algorithm " + algorithm.getName());
		}

		Neo4jJob job = new Neo4jJob(platformConfig, "Vertex", "EDGE", Collections.emptyMap(), cypher, graphName, config, outputPath);
		job.execute();

		LOG.info("Executed benchmark with algorithm \"{}\" on graph \"{}\".",
				benchmarkRun.getAlgorithm().getName(),
				benchmarkRun.getFormattedGraph().getName());
	}

	@Override
	public BenchmarkMetrics finalize(RunSpecification runSpecification) throws Exception {
		Neo4jCollector.stopPlatformLogging();
		BenchmarkRunSetup benchmarkRunSetup = runSpecification.getBenchmarkRunSetup();
		Path logDir = benchmarkRunSetup.getLogDir().resolve("platform");

		BenchmarkMetrics metrics = new BenchmarkMetrics();
		metrics.setProcessingTime(Neo4jCollector.collectProcessingTime(logDir));
		return metrics;
	}

	@Override
	public void terminate(RunSpecification runSpecification) {
		BenchmarkRunner.terminatePlatform(runSpecification);
	}

	@Override
	public String getPlatformName() {
		return PLATFORM_NAME;
	}
}
