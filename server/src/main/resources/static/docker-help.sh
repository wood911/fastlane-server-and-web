#安装docker
https://docs.docker.com/engine/install/centos

yum install -y yum-utils
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

systemctl start/stop docker
docker run hello-world
#开机自启动
sudo systemctl enable docker

#卸载docker
sudo yum remove docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin docker-ce-rootless-extras
sudo rm -rf /var/lib/docker
sudo rm -rf /var/lib/containerd

docker system df
docker ps -a
docker images
docker rm/rmi id
docker info
docker stats
docker system prune --volumes
docker system prune --all


#拉取最新 Halo 镜像 https://simplestark.com/archives/-jing-yan-bao-bao-halobo-ke-qian-yi
sudo docker pull ruibaby/halo
#-it：开启输入功能并连接伪终端
#-d：后台运行容器
#--name：为容器指定一个名称
#-p：端口映射，格式为 主机(宿主)端口:容器端口 ，可在 application.yaml 配置
#-v：工作目录映射。形式为：-v 宿主机路径:/root/.halo2，后者不能修改
#--restart：建议设置为 unless-stopped，在 Docker 启动的时候自动启动 Halo 容器
docker run -it -d --name halo -p 8090:8090 -v ~/.halo:/root/.halo --restart=unless-stopped ruibaby/halo

docker run -it -d --name app -p 8095:8095 -v /root/appsign:/root/appsign --restart=unless-stopped app

#拉取最新 MySQL 镜像
sudo docker pull mysql:5.7
#创建容器
docker run -it -d --name mysql -p 3306:3306 --restart=unless-stopped -e MYSQL_ROOT_PASSWORD=Wutf@19930524 mysql:5.7


# 备份数据库
docker exec -it mysql mysqldump -uroot -pWutf@19930524 halodb > /root/.halo/halodb.sql
# 将需要执行的sql cp 到目标容器，也就是下面:mysql
docker cp /root/.halo/halodb.sql  mysql:halodb.sql
# 进入目标容器
docker exec -it mysql /bin/bash
# 登陆mysql
mysql -uroot -pWutf@19930524
# 创建数据库
create database halodb;
# 使用 halodb
use halodb;
# 执行 sql
source halodb.sql

#获取docker内部ip用于MySQL连接
# url: jdbc:mysql://172.17.0.2:3306/halodb 这里用下面的ip
docker inspect --format='{{.NetworkSettings.IPAddress}}' mysql

docker logs -f --tail 100 xxx




#开放防火墙端口 https://www.digitalocean.com/community/tutorials/opening-a-port-on-linux
systemctl status firewalld
sudo firewall-cmd --zone=public --permanent --add-port=80/tcp
sudo firewall-cmd --zone=public --permanent --add-port=443/tcp
sudo firewall-cmd --reload
firewall-cmd --list-ports
#列出所有开放端口
netstat -lntu

#pem转换为crt
openssl x509 -outform der -in cert.pem -out cert.crt
#从pem文件中提取密钥
openssl pkey -in private.pem -out private.key

openssl smime -sign -in unsigned.mobileconfig -out udid.mobileconfig -signer cert.crt -inkey private.key -certfile chain.crt -outform der -nodetach


