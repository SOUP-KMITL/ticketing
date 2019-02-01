#!/bin/bash
pipenv install  --python python3.7 $1
pipenv lock -r --python python3.7 > requirements.txt
