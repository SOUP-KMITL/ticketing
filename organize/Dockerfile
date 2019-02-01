FROM python:3.7
ADD ./requirements.txt /requirements.txt
RUN pip install -r /requirements.txt
ADD ./ /
CMD gunicorn -w 4 -b :80 -k gunicorn.workers.ggevent.GeventWorker --max-requests 1000 --keep-alive 60 app:app