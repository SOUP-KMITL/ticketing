#!/bin/bash
docker service update --publish-rm 80:80 -d ticketing_kong
sleep 20s
sudo certbot renew --preferred-challenges http 
sudo cp -rH /etc/letsencrypt/live/api.smartcity.kmitl.io/* /home/centos/ticketing/cert
scp -r /home/centos/ticketing/cert/* worker-03:glusterdata/cert
sudo docker stack deploy -c /home/centos/ticketing/docker-compose.yml ticketing