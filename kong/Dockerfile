FROM kong:0.12
COPY custom_nginx.template /custom_nginx.template
CMD kong start --nginx-conf /custom_nginx.template -vv


