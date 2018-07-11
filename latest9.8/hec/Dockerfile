
FROM ubuntu:latest

MAINTAINER O'Neil Delpratt <oneil@saxonica.com>

RUN apt-get update && apt-get -y upgrade && DEBIAN_FRONTEND=noninteractive apt-get -y install wget unzip apache2 php7.2 php7.2-dev libapache2-mod-php7.2

WORKDIR /
RUN a2enmod php7.2
RUN a2enmod rewrite

RUN wget http://www.saxonica.com/saxon-c/libsaxon-HEC-setup64-v1.1.0.zip && unzip libsaxon-HEC-setup64-v1.1.0.zip
RUN yes "" | ./libsaxon-HEC-setup64-v1.1.0
RUN ln -s /Saxonica/Saxon-HEC1.1.0/samples/php /var/www/html/
RUN ln -s /usr/lib/ /usr/lib64
RUN ln -s /Saxonica/Saxon-HEC1.1.0/libsaxonhec64.so /usr/lib/libsaxonhec64.so
RUN ln -s /Saxonica/Saxon-HEC1.1.0/rt/ /usr/lib/rt
RUN export LD_LIBRARY_PATH=/usr/lib/rt/lib/amd64:$LD_LIBRARY_PATH
RUN ldconfig

WORKDIR /Saxonica/Saxon-HEC1.1.0/Saxon.C.API/

RUN wget https://dev.saxonica.com/repos/archive/opensource/latest9.8/hec/Saxon.C.API/jni/jni.h
RUN wget https://dev.saxonica.com/repos/archive/opensource/latest9.8/hec/Saxon.C.API/jni/jni_md.h 

RUN phpize && ./configure --enable-saxon CPPFLAGS="-Ijni" && make && make install
RUN echo extension=saxon.so >> /etc/php/7.2/apache2/conf.d/saxon.ini
RUN echo extension=saxon.so >> /etc/php/7.2/mods-available/saxon.ini
RUN phpenmod saxon
RUN php -m | grep saxon -i
RUN echo export LD_LIBRARY_PATH=/usr/lib/rt/lib/amd64:$LD_LIBRARY_PATH >> /etc/apache2/envvars 
# CMD ["apachectl", "-DFOREGROUND"]

RUN sed -i "s/short_open_tag = Off/short_open_tag = On/" /etc/php/7.2/apache2/php.ini
RUN sed -i "s/error_reporting = .*$/error_reporting = E_ERROR | E_WARNING | E_PARSE/" /etc/php/7.2/apache2/php.ini

# Manually set up the apache environment variables
ENV APACHE_RUN_USER www-data
ENV APACHE_RUN_GROUP www-data
ENV APACHE_LOG_DIR /var/log/apache2
ENV APACHE_LOCK_DIR /var/lock/apache2
ENV APACHE_PID_FILE /var/run/apache2.pid

# Expose apache.
EXPOSE 80

# Copy this repo into place.
#ADD samples/php /var/www/html

# Update the default apache site with the config we created.
ADD apache-config.conf /etc/apache2/sites-enabled/000-default.conf

# By default start up apache in the foreground, override with /bin/bash for interative.
CMD /usr/sbin/apache2ctl -D FOREGROUND
