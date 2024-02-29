#/bin/sh

set -e

GRAPHS_DIR=${1:-~/graphs}
NEO4J_DIR=${2:-~/neo4j}

PROJECT=graphalytics-1.10.0-neo4j-0.1-SNAPSHOT

rm -rf $PROJECT
mvn package -U -DskipTests -Dmaven.buildNumber.skip
tar xf $PROJECT-bin.tar.gz
cd $PROJECT/
cp -r config-template config
sed -i "s|^graphs.root-directory =$|graphs.root-directory = $GRAPHS_DIR|g" config/benchmark.properties
sed -i "s|^graphs.validation-directory =$|graphs.validation-directory = $GRAPHS_DIR|g" config/benchmark.properties
sed -i "s|^platform.neo4j.home =$|platform.neo4j.home = $NEO4J_DIR|g" config/platform.properties
sed -i "s|^benchmark.custom.graphs = .*$|benchmark.custom.graphs = test-pr-directed|g" config/benchmarks/custom.properties
sed -i "s|^benchmark.custom.algorithms = .*$|benchmark.custom.algorithms = PR|g" config/benchmarks/custom.properties
