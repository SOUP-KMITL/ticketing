FROM python:3.7-rc
COPY ./ /
RUN pip install -r /requirements.txt
CMD uwsgi --http-socket :80 --wsgi-file /ac_service.py --master --processes 4 --threads 2