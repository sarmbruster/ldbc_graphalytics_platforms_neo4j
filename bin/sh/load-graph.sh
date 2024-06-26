#!/bin/bash
#
# Copyright 2015 Delft University of Technology
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

rootdir=$(dirname $(readlink -f ${BASH_SOURCE[0]}))/../..

# Parse commandline instructions (provided by Graphalytics).
while [[ $# -gt 1 ]] # Parse two arguments: [--key value] or [-k value]
  do
  key="$1"
  value="$2"

  case ${key} in

    --neo4j-home)
      NEO4J_HOME="$value"
      shift;;

    --graph-name)
      GRAPH_NAME="$value"
      shift;;

    --input-vertex-path)
      INPUT_VERTEX_PATH="$value"
      shift;;

    --input-edge-path)
      INPUT_EDGE_PATH="$value"
      shift;;

    --output-path)
      OUTPUT_PATH="$value"
      shift;;

    --directed)
      DIRECTED="$value"
      shift;;

    --weighted)
      WEIGHTED="$value"
      shift;;

    *)
      echo "Error: invalid option: " "$key"
      exit 1
      ;;
  esac
  shift
done

HEADER_NODES=$(mktemp)
echo "VID:ID" >> ${HEADER_NODES}
HEADER_RELATIONSHIPS=$(mktemp)

case ${WEIGHTED} in
    "true")
        echo ":START_ID :END_ID WEIGHT:DOUBLE" >> ${HEADER_RELATIONSHIPS}
        ;;
    "false")
        echo ":START_ID :END_ID" >> ${HEADER_RELATIONSHIPS}
        ;;
    *)
        echo "Bad weight parameter" >&2
	    exit 1
esac

${NEO4J_HOME}/bin/neo4j-admin database import full \
  --id-type=INTEGER \
  --nodes=Vertex="${HEADER_NODES},${INPUT_VERTEX_PATH}" \
  --relationships=EDGE="${HEADER_RELATIONSHIPS},${INPUT_EDGE_PATH}" \
  --delimiter ' ' \
  --overwrite-destination \
  $GRAPH_NAME

echo "CREATE DATABASE \`${GRAPH_NAME}\`;" | ${NEO4J_HOME}/bin/cypher-shell

#rm ${HEADER_NODES}
#rm ${HEADER_RELATIONSHIPS}