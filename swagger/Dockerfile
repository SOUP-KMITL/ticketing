FROM swaggerapi/swagger-ui
COPY data-exchange-swagger.json /doc/data-exchange/data-exchange-swagger.json
RUN sed -i '/presets: \[/ i \\t validatorUrl: null,' /usr/share/nginx/html/index.html && \
    sed -i 's/Swagger UI/Data Exchange Swagger/g' /usr/share/nginx/html/index.html &&\
    sed -i 's/location \//location \/doc\/data-exchange/g' /etc/nginx/nginx.conf && \
    sed -i 's/root/alias/g' /etc/nginx/nginx.conf  && \
    sed -i 's|\./$(basename $SWAGGER_JSON)|$SWAGGER_JSON|g' /usr/share/nginx/docker-run.sh 


