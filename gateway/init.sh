#!/usr/bin/bash
sudo yum update -y
sudo yum install -y yum-utils \
  device-mapper-persistent-data \
  lvm2 -y
sudo yum-config-manager \
    --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo -y
sudo yum install docker-ce -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER
sudo yum install git -y
# Install compose
sudo curl -L "https://github.com/docker/compose/releases/download/1.22.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
sudo docker-compose up -d
# docker run --network=kong-net \
#     --name kong-certbot-agent \
#     --restart=always \
#     -e "KONG_ENDPOINT=http://kong:8001" \
#     -e "EMAIL=support@smartcity.kmitl.io" \
#     -e "DOMAINS=api2.smartcity.kmitl.io" \
#     phpdockerio/kong-certbot-agent 