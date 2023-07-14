## Spring Cloud Alibaba

1. feature

   - 服务注册与发现:
     - 适配 springcloud 服务注册与发现标准, 默认集成了 Ribbon
   - 服务限流降级:
     - 默认支持 Servlet, Feign, RestTemplate, Dubbo, RocketMQ,
     - 可以在运行时通过控制台实时的修改限流降级规则
     - 还支持查看限流降级 Metrics 监控
   - 分布式配置管理:
     - 支持分布式系统的外部化配置支持, 配置更改时自动刷新
   - 消息驱动能力:
     - 基于 springcloud 为微服务构建消息驱动能力
   - 阿里云对象存储:
   - 分布式任务调度: 提供秒级别的, 精准的, 高可靠, 高可用的定时任务调度
     - 提供分布式任务调度模型, 如网格任务: 支持海量任务均匀的分布到所有的 worker 上执行

2. production

   - Nacos[Naming Configuration Service]: `服务注册中心 + 服务配置中心`
     - `Nacos = Eureka + **Config + Bus**`
   -

### nacos

1. stand alone: install or run in docker

   ```yml
   nacos:
     image: nacos/nacos-server:1.1.4
     container_name: dev-nacos-standalone
     environment:
       - PREFER_HOST_MODE=hostname
       - MODE=standalone
     volumes:
       - /root/nacos/log/standalone-logs/:/home/nacos/logs
       - /root/nacos/config/init.d/custom.properties:/home/nacos/init.d/custom.properties
     ports:
       - '8848:8848'
   # http://101.132.45.28:8848/nacos/
   ```

   - use mysql

   ```yml
   nacos:
     image: registry.cn-shanghai.aliyuncs.com/alice52/dev-nacos:20200412.8bcaddc
     container_name: dev-nacos-standalone
     environment:
       - PREFER_HOST_MODE=hostname
       - MODE=standalone
     volumes:
       - /root/nacos/log/standalone-logs/:/home/nacos/logs
     ports:
       - '8848:8848'
   ```

2. [discovery](./02.服务注册中心.md/#4-nacosrecommend)
3. [config](./06.服务配置.md/#2-nacosrecommend)
4. deploy kind

   - stand alone
   - cluster
   - multi cluster

5. nacos 集群

![avatar](/static/image/spring/alibab-nacos-cluster.png)

- 集群 compose

```yml
version: '2.1'
# doonot use this for some error: page not find
#networks:
#  nacos_net:
#    driver: bridge
services:
  # nacos-server服务注册与发现，配置中心服务
  nacos-server-cluster:
    image: registry.cn-shanghai.aliyuncs.com/alice52/dev-nacos:20200412.8bcaddc
    container_name: nacos-server
    ports:
      - '8848:8848'
      - '9555:9555'
    # doonot use this for some error: page not find
    #networks:
    #  - 'nacos_net'
    restart: 'no'
    privileged: true
    environment:
      #如果支持主机名可以使用hostname,否则使用ip，默认也是ip
      PREFER_HOST_MODE: ip
      #数据源平台 仅支持mysql或不保存empty
      SPRING_DATASOURCE_PLATFORM: mysql
      #多网卡情况下，指定ip或网卡
      NACOS_SERVER_IP: 101.132.45.28
      #集群中其它节点[ip1:port ip2:port ip3:port]
      NACOS_SERVERS: 101.37.174.197:8848 101.132.45.28:8848 121.36.195.5:8848
      #mysql配置，Master为主节点，Slave为从节点
      MYSQL_MASTER_SERVICE_HOST: 101.132.45.28
      MYSQL_MASTER_SERVICE_PORT: 3306
      MYSQL_MASTER_SERVICE_DB_NAME: nacos_dev
      MYSQL_MASTER_SERVICE_USER: root
      MYSQL_MASTER_SERVICE_PASSWORD: ***
      # MYSQL_SLAVE_SERVICE_HOST: 101.37.174.197
      # MYSQL_SLAVE_SERVICE_PORT: 3306
      #JVM调优参数
      JVM_XMS: 512m # default 2g
      JVM_XMX: 512m # default :2g
      JVM_XMN: 512m # default :1g
      JVM_MS: 128m #default :128m
      JVM_MMS: 320m # default :320m
      #NACOS_DEBUG: n #是否开启远程debug，y/n，默认n
      #TOMCAT_ACCESSLOG_ENABLED: true #是否开始tomcat访问日志的记录，默认false
    volumes:
      - /root/nacos/cluster/logs:/home/nacos/logs
```

- nginx

- nginx.conf

```conf
# notice: upstream should be place in nginx.conf

#gzip  on;
http {
  ...

  upstream cluster {
      server 121.36.195.5:8848;
      server 101.132.45.28:8848;
      server 101.37.174.197:8848;
  }
}
```

- default.conf

```conf
location / {
   # root   /usr/share/nginx/html;
   # index  index.html index.htm;
   proxy_pass http://cluster;
}
```

6. nacos 持久化

   - 默认的 nacos 自带内嵌式的数据存储[derby]
   - use mysl to store data
   - config

   ```properties
   # spring
   server.contextPath=/nacos
   server.servlet.contextPath=/nacos
   server.port=8848

   # nacos.cmdb.dumpTaskInterval=3600
   # nacos.cmdb.eventTaskInterval=10
   # nacos.cmdb.labelTaskInterval=300
   # nacos.cmdb.loadDataAtStart=false

   # metrics for prometheus
   #management.endpoints.web.exposure.include=*

   # metrics for elastic search
   management.metrics.export.elastic.enabled=false
   #management.metrics.export.elastic.host=http://localhost:9200

   # metrics for influx
   management.metrics.export.influx.enabled=false
   #management.metrics.export.influx.db=springboot
   #management.metrics.export.influx.uri=http://localhost:8086
   #management.metrics.export.influx.auto-create-db=true
   #management.metrics.export.influx.consistency=one
   #management.metrics.export.influx.compressed=true

   server.tomcat.accesslog.enabled=true
   server.tomcat.accesslog.pattern=%h %l %u %t "%r" %s %b %D %{User-Agent}i
   # default current work dir
   server.tomcat.basedir=

   ## spring security config
   ### turn off security
   #spring.security.enabled=false
   #management.security=false
   #security.basic.enabled=false
   #nacos.security.ignore.urls=/**

   nacos.security.ignore.urls=/,/**/*.css,/**/*.js,/**/*.html,/**/*.map,/**/*.svg,/**/*.png,/**/*.ico,/console-fe/public/**,/v1/auth/login,/v1/console/health/**,/v1/cs/**,/v1/ns/**,/v1/cmdb/**,/actuator/**,/v1/console/server/**

   # nacos.naming.distro.taskDispatchPeriod=200
   # nacos.naming.distro.batchSyncKeyCount=1000
   # nacos.naming.distro.syncRetryDelay=5000
   # nacos.naming.data.warmup=true
   # nacos.naming.expireInstance=true

   nacos.istio.mcp.server.enabled=false

   # custom
   spring.datasource.platform=mysql

   db.num=1
   db.url.0=jdbc:mysql://101.132.45.28:3306/nacos_devtest?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true
   db.user=root
   db.password=***
   ```

### Sentinel

- [reference](04.服务降级.md/#3-sentinelrecommend)

### seata

---

## reference

1. [alibaba cloud](https://spring-cloud-alibaba-group.github.io/github-pages/greenwich/spring-cloud-alibaba.html)
