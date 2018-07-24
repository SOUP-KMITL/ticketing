FROM python
ADD ./requirements.txt /requirements.txt
RUN pip install -r /requirements.txt
ADD ./ /
CMD gunicorn -w 4 -b 127.0.0.1:80 -b :80 -k gunicorn.workers.ggevent.GeventWorker --max-requests 1000 --keep-alive 60 ac_service:application