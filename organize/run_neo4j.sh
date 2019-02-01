#!/bin/bash
sudo systemctl start docker
docker run \
    --publish=7474:7474 --publish=7687:7687 \
    --rm \
    --env=NEO4J_AUTH=none \
    neo4j
sudo systemctl stop docker