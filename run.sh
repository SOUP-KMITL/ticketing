#!/bin/bash
while getopts ":d" o; do
    case "${o}" in
        d)
            mongo CollectionModel --eval "db.dropDatabase()"
            mongo DataExchange --eval "db.dropDatabase()"
            sudo docker service rm ticketing_neo4j
            ;;
        *)
            echo Invaild options
            exit 1
            ;;
    esac
done
sudo docker login -u athiyutbest -p Bladenight1
sudo docker-compose build
sudo docker-compose push  
sudo docker stack deploy -c docker-compose.yml ticketing
# ansible cluster -m shell -a "docker system prune -f"
# sudo docker system prune -f
exit 0