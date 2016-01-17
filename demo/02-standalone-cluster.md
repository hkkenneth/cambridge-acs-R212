# Spark demo - standalone mode

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

### Starting the cluster using manual connection

```bash
# On master
./sbin/start-master.sh


# or on slaves
./sbin/start-slave.sh spark://<master_private_ip>:7077

# Shut down
./sbin/stop-master.sh
./sbin/stop-slave.sh
```

### Starting the cluster using configuration file

```bash
# On master: edit list of slaves - one ip per line
vi conf/slaves

# Set up passphraseless so master can ssh to slaves
# On master
ssh-keygen -t dsa -P '' -f ~/.ssh/id_dsa
cat ~/.ssh/id_dsa.pub
# Copy the id_dsa.pub line and paste to the end of ~/.ssh/authorized_keys on every slave
vi ~/.ssh/authorized_keys

# On master
./sbin/start-all.sh
```

Access the cluster master UI at http://master_ip:8080/
Access the cluster slave UI at http://slave_ip:8081/

### Run Spark interactive shell on client

Run Spark Scala shell

```bash
./bin/spark-shell --master spark://master_private_ip:7077
```

Check if you can access the appliction's monitoring interface at
http://your_public_ip:4040 .

Exit the shell if it works well.

### Set up HDFS
Set up according to https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/ClusterSetup.html

```
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

# On master: edit host list
vi etc/hadoop/slaves

# On master: Format HDFS
bin/hdfs namenode -format

# On master: Start HDFS, you may be asked for confirmation of authenticity of host (for ssh)
$HADOOP_PREFIX/sbin/start-dfs.sh
```

The HDFS monitoring UI can be accessed at http://ip:50070/

### Run a sample program

1. Generate a file consisting of 400,000,000 random 8-character strings. File size should be about 3.4GB. Upload data file to HDFS.

  ```bash
  cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 8 | head -n 400000000 > datafile.txt
  $HADOOP_PREFIX/bin/hdfs dfs -mkdir /spark
  $HADOOP_PREFIX/bin/hdfs dfs -put datafile.txt /spark/datafile.txt
  ```

2. Start the Spark Scala shell again

  ```bash
  ./bin/spark-shell --master spark://master_ip:7077 --executor-memory=12G
  ```

3. Run the sample program in shell

  ```scala
  val dataFile = "hdfs://namenode_ip:9000/spark/datafile.txt"
  val data = sc.textFile(dataFile).cache()
  val numAs = data.filter(line => line.contains("a")).count()
  val numBs = data.filter(line => line.contains("b")).count()
  println("Lines with a: %s, Lines with b: %s".format(numAs, numBs))
  ```

### Run our benchmark program

```scala
val dataFile = "hdfs://namenode_ip:9000/spark/datafile.txt"
val data = sc.textFile(dataFile)

val groupedData = data.flatMap(line => Array((line.substring(0, 3), line),
                                             (line.substring(1, 4), line))).groupByKey()
val groupedData2 = groupedData.flatMap{case (a, b) => b}.map(line => (line.substring(4, 8), line)).groupByKey()
val countBySuffix = groupedData2.map{case (a, b) => b.size}
countBySuffix.collect()

// The following should finish in shorter time.
val countBySuffixPlusOne = groupedData2.map{case (a, b) => b.size + 1}
countBySuffixPlusOne.collect()
```

### Enable KryoSerializer

```bash
export SPARK_JAVA_OPTS="-Dspark.serializer=org.apache.spark.serializer.KryoSerializer"
```
