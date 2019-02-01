FROM swaggerapi/swagger-ui
COPY data-exchange-swagger.json /data-exchange/swagger.json
COPY index.html /usr/share/nginx/html/index.html
# COPY data-exchange-swagger.json /docs/data-exchange-swagger.json
RUN sed -i 's/root/alias/g' /etc/nginx/nginx.conf && \
    # sed -i 's|\./$(basename $SWAGGER_JSON)|$SWAGGER_JSON|g' /usr/share/nginx/docker-run.sh && \
    sed -i "s/8080/80/"  /etc/nginx/nginx.conf && \
    mkdir /tmp/data-exchange && \
    cp -r /usr/share/nginx/html/* /tmp/data-exchange && \
    mv  /tmp/data-exchange /usr/share/nginx/html/


