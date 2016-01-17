# Spark demo - Mesos mode

### Download dependencies (on all hosts)

```bash
sudo apt-get update
sudo apt-get install default-jdk
```

### Download Mesos binary from mesosphere

Follow https://mesosphere.com/downloads/ and https://open.mesosphere.com/getting-started/install/

```bash
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF
DISTRO=$(lsb_release -is | tr '[:upper:]' '[:lower:]')
CODENAME=$(lsb_release -cs)

echo "deb http://repos.mesosphere.io/${DISTRO} ${CODENAME} main" | \
  sudo tee /etc/apt/sources.list.d/mesosphere.list
sudo apt-get -y update

sudo apt-get -y install mesos

# Set /etc/zookeeper/conf/myid to a unique integer between 1 and 255 on each node.
sudo vi /etc/zookeeper/conf/myid

# Set Server Addresses
sudo vi /etc/zookeeper/conf/zoo.cfg

# Restart Zookeeper
sudo service zookeeper restart

# Set Mesos' ZooKeeper config
sudo vi /etc/mesos/zk
sudo vi /etc/mesos-master/quorum

# Restart master
sudo service mesos-master restart
```

Mesos cluster UI: http://master_ip:5050

### Set up HDFS
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
```

The HDFS monitoring UI can be accessed at http://ip:50070/


### Download Spark

Obtain latest version of Spark from http://spark.apache.org/downloads.html

* Choose a Spark release: 1.6.0 (Jan 04 2016)
* Choose a package type:  Pre-built for Hadoop 2.6 and later
  ```bash
  wget http://d3kbcqa49mib13.cloudfront.net/spark-1.6.0-bin-hadoop2.6.tgz
  tar xzpf spark-1.6.0-bin-hadoop2.6.tgz
  cd spark-1.6.0-bin-hadoop2.6/

  # Upload the binary to HDFS
  ./hadoop-2.6.3/bin/hadoop dfs -put spark-1.6.0-bin-hadoop2.6.tgz /
  ```

## Configure Spark for Mesos

```bash
# Find the mesos shared library
ls /usr/local/lib/libmesos.so
/usr/local/lib/libmesos.so

vi conf/spark-env.sh
export MESOS_NATIVE_LIBRARY=/usr/local/lib/libmesos.so
export SPARK_EXECUTOR_URI=hdfs://ip-172-31-25-189.us-west-2.compute.internal:9000/spark-1.6.0-bin-hadoop2.6.tgz
```

### Run Spark shell

```bash
./bin/spark-shell --master mesos://zk://master_ip:2181
```
