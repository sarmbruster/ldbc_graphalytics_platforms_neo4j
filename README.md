# Graphalytics Neo4j platform driver

Neo4j implementation of the LDBC Graphalytics benchmark. 
This implementation expects that a running Neo4j instance including GDS library is provided externally.

To run the benchmark, follow the steps in the Graphalytics tutorial on [Running Benchmark](https://github.com/ldbc/ldbc_graphalytics/wiki/Manual%3A-Running-Benchmark) with the Neo4j-specific instructions listed below.

### building the benchmark


To build the benchmarks you need to have a Java JDK 21 installed and [Maven](https://maven.apache.org/) as well.

#### Java installation
```bash
sudo apt install libc6-i386 libc6-x32 libxi6 libxtst6 -y
wget https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.deb
sudo dpkg -i jdk-21_linux-x64_bin.deb
export JAVA_HOME=/usr/lib/jvm/jdk-21-oracle-x64
export PATH="$JAVA_HOME/bin:$PATH"
```

#### Maven installation
```bash
wget https://mirrors.estointernet.in/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
tar -xvf apache-maven-3.6.3-bin.tar.gz
sudo mv apache-maven-3.6.3 /opt/
export M2_HOME='/opt/apache-maven-3.6.3'
export PATH="$M2_HOME/bin:$PATH"
```

To initialize the benchmark package, run:

```bash
./init.sh MY_GRAPH_DIR NEO4J_DIR 
```
where

* `MY_GRAPH_DIR` should point to the directory of the graphs and the validation data. The default value is `~/graphs`.
* `NEO4J_DIR` should point to Neo4j's directory. The default value is `~/neo4j`.

I've been using
```bash
 ./init.sh ../../data ~/Downloads/neo4j-enterprise-5.16.0    
 ```

This command builds the project and create a self-contained tar.gz. 
Additionally it generates appropriate configuration files in the extracted folder which you can adopt to your needs.
Switch into the `graphalytics-1.10.0-neo4j-0.1-SNAPSHOT` folder to later run and configure the benchmarks.


### Installing and configuring Neo4j

As a prerequisite Neo4j Enterprise >= 5.x must be installed.
Community edition is not good enough since the benchmarks populate separate databases.
Additionally the [GDS library](https://neo4j.com/docs/graph-data-science/current/) needs to be installed.

> [!WARNING]  
> Without a license file GDS will only use up to 4 CPU cores. 

To get Neo4j and install it to the default location including GDS library, run:

```bash
wget https://dist.neo4j.org/neo4j-enterprise-5.16.0-unix.tar.gz
wget https://graphdatascience.ninja/neo4j-graph-data-science-2.6.1.zip
tar zxf neo4j-enterprise-5.16.0-unix.tar.gz
cd neo4j-enterprise-5.16.0/plugins
unzip ../../neo4j-graph-data-science-2.6.1.zip
cd ..
echo 'dbms.security.procedures.unrestricted=gds.*,apoc.*' >> conf/neo4j.conf
echo 'dbms.security.auth_enabled=false' >> conf/neo4j.conf
cd ..
mv neo4j-enterprise-5.16.0/ ~/neo4j/
~/neo4j/bin/neo4j start
```

You might also tweak the memory setting (min_heap, max_heap, pagecache) in `conf/neo4j.conf`.
After a few seconds neo4j should be reachable on http://localhost:7474. 
You don't need a username or password to log in.

### downloading datasets

Prepared datasets including result validation data can be downloaded from https://ldbcouncil.org/benchmarks/graphalytics/.
Place the extracted files into the `MY_GRAPH_DIR` folder (see above).

### configuring and running the benchmarks

The benchmark configuration is read from `config` subfolder, the main config file is `benchmark.properties` which uses couple of includes of other files.
The config files contain all the graph definitions, which algorithms to run, if results should be validated and many more.

Once configuration is done benchmarks are run with

```bash
bin/sh/run-benchmark.sh
```

> [!TIP]
> Use a tool like `tmux` if running benchmarks on a remote server so you don't have to keep the connection up.

### result validation

With config option `benchmark.custom.validation-required` result validation can be enabled/disabled.

### gotchas

#### reports

When a benchmark is run the results are written to `report/<timestamp>/...` folder. 
For a unknown reason the HTML reports do not contain the actual data. 
Since the data itself is available in json format I haven't further investigated on this

#### validation

For a unknown reason the Neo4j results for PageRank (and maybe others) are not fitting the expected value close enough.
Therefore it's a good idea to switch off result validation otherwise the runtime of the benchmarks are not displayed.


### hints

* for debugging the main java process: 

```bash
export java_opts='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005'
```
* to debug the java process that is started from the main one for each job you need a modification to the graphalytics library, see ....
Once this used you can switch on remote debugging:

```bash
export FORKED_JAVA_TOOL_OPTIONS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006'
```


