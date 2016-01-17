# Spark demo - YARN mode

### Download dependencies (on all hosts)

```bash
sudo apt-get update
sudo apt-get install default-jdk
```

### Download Spark (on all hosts)

Obtain latest version of Spark from http://spark.apache.org/downloads.html

* Choose a Spark release: 1.6.0 (Jan 04 2016)
* Choose a package type:  Pre-built for Hadoop 2.6 and later
  ```bash
  wget http://d3kbcqa49mib13.cloudfront.net/spark-1.6.0-bin-hadoop2.6.tgz
  tar xzpf spark-1.6.0-bin-hadoop2.6.tgz
  cd spark-1.6.0-bin-hadoop2.6/
  ```

### Set up HDFS and YARN
Set up according to https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/ClusterSetup.html

```bash
# On every hosts
# Download dependencies
sudo apt-get install ssh
sudo apt-get install rsync

# Download Hadoop
wget http://mirror.ox.ac.uk/sites/rsync.apache.org/hadoop/common/hadoop-2.6.3/hadoop-2.6.3.tar.gz
tar xzpf hadoop-2.6.3.tar.gz

# Find the location of java binary
which java
# /usr/bin/java

# set to the root of your Java installation
export JAVA_HOME=/usr

cd hadoop-2.6.3/
export HADOOP_PREFIX=`pwd`
export HADOOP_CONF_DIR=$HADOOP_PREFIX/etc/hadoop
export HADOOP_YARN_HOME=$HADOOP_PREFIX

# edit JAVA_HOME
vi etc/hadoop/hadoop-env.sh

# edit HDFS configuration
# fs.defaultFS
vi etc/hadoop/core-site.xml
# dfs.replication
vi etc/hadoop/hdfs-site.xml
# yarn.resourcemanager.hostname
vi etc/hadoop/yarn-site.xml

# On master: edit host list
vi etc/hadoop/slaves

# Set up passphraseless so master can ssh to slaves
# On master
ssh-keygen -t dsa -P '' -f ~/.ssh/id_dsa
cat ~/.ssh/id_dsa.pub
# Copy the id_dsa.pub line and paste to the end of ~/.ssh/authorized_keys on every slave
vi ~/.ssh/authorized_keys

# On master: Format HDFS
bin/hdfs namenode -format

# On master: Start HDFS, you may be asked for confirmation of authenticity of host (for ssh)
$HADOOP_PREFIX/sbin/start-dfs.sh
# On master: Start YARN
$HADOOP_PREFIX/sbin/start-yarn.sh
```

The HDFS monitoring UI can be accessed at http://ip:50070/
The Hadoop cluster UI can be accessed at http://ip:8088/

### Run shell in YARN client mode

The client can request number of executors and CPU cores and memory needed.

```bash
$ ./bin/spark-shell --master yarn-client --num-executors 3 --driver-memory 2g --executor-memory 4g --executor-cores 2
```

Note that in YARN mode, the usual Spark UI at http://ip:4040/ would be redirect to the YARN proxy
http://ip:8088/cluster/app/application_id

### Run our benchmark program

```scala
/* SimpleApp.scala */
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

object SimpleApp {
  def main(args: Array[String]) {
    val dataFile = "hdfs://ip-172-31-25-189.us-west-2.compute.internal:9000/spark/datafile.txt"
    val conf = new SparkConf().setAppName("Simple Application")
    val sc = new SparkContext(conf)
    val data = sc.textFile(dataFile)
    val groupedData = data.flatMap(line => Array((line.substring(0, 3), line),
                                                 (line.substring(1, 4), line))).groupByKey()
    val groupedData2 = groupedData.flatMap{case (a, b) => b}.map(line => (line.substring(4, 8), line)).groupByKey()
    val countBySuffix = groupedData2.map{case (a, b) => b.size}
    val a = countBySuffix.collect()

    // The following should finish in shorter time.
    val countBySuffixPlusOne = groupedData2.map{case (a, b) => b.size + 1}
    val b = countBySuffixPlusOne.collect()
  }
}
```

```bash
./bin/spark-submit --class "SimpleApp" \
    --master yarn-cluster \
    --num-executors 3 \
    --driver-memory 2g \
    --executor-memory 4g \
    --executor-cores 2 \
    quick-start/target/scala-2.10/simple-project_2.10-1.0.jar  > stdout.log 2> stderr.log
```
