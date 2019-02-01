#!//usr/bin/bash
sudo docker run --network=gateway_kong-net \
    --name kong-certbot-agent \
    --rm \
    phpdockerio/kong-certbot-agent \
    ./certbot-agent certs:update \
    http://kong:8001 \
    support@smartcity.kmitl.io \
    api.smartcity.kmitl.io,developers.smartcity