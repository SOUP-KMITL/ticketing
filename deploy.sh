#!/bin/bash
rsync -azrP -e "ssh -A dga ssh" ../ticketing centos@worker1:
ssh dga 'ssh centos@worker1 "cd ticketing && ./run.sh"'
exit 0
