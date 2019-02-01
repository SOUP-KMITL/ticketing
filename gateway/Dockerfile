FROM kong:1.0.1
COPY custom_nginx.template /custom_nginx.template
CMD kong start --nginx-conf /custom_nginx.template -vv


