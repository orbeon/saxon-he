FROM php:7.2-apache

MAINTAINER O'Neil Delpratt <oneil@saxonica.com>

RUN apt-get -y update && apt-get -y install gcj-jdk wget unzip

WORKDIR /saxonica/
RUN cd /saxonica

RUN wget http://www.saxonica.com/saxon-c/libsaxon-HEC-setup64-v1.1.0.zip && unzip libsaxon-HEC-setup64-v1.1.0.zip
RUN yes "" | ./libsaxon-HEC-setup64-v1.1.0
RUN ln -s /usr/lib/ /usr/lib64
RUN ln -s /saxonica/Saxonica/Saxon-HEC1.1.1/libsaxoneec.so /usr/lib/libsaxoneec.so
RUN ln -s /saxonica/Saxonica/Saxon-HEC1.1.0/rt/ /usr/lib/rt
RUN export LD_LIBRARY_PATH=/usr/lib/rt/lib/amd64:$LD_LIBRARY_PATH
RUN ldconfig

RUN cd Saxonica/Saxon-HEC1.1.0/Saxon.C.API/ && phpize && ./configure --enable-saxon && make && make install
RUN echo extension=saxon.so >> /usr/local/etc/php/conf.d/saxon.ini
#RUN phpenmod saxon
RUN echo export LD_LIBRARY_PATH=/usr/lib/rt/lib/amd64:$LD_LIBRARY_PATH >> /etc/apache2/envvars CMD ["apachectl", "-DFOREGROUND"]
RUN php -m | grep saxon -i
