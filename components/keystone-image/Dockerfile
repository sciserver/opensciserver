FROM python:3.9-bullseye

ENV SERVER_VERSION=26.0.0

RUN apt-get -y update \
    && apt-get install -y libev-dev libffi-dev python-dev libssl-dev default-mysql-client memcached \
    && apt-get -y clean

COPY requirements.txt /opt/keystone-requirements.txt

RUN git clone -b ${SERVER_VERSION} https://github.com/openstack/keystone.git /opt/keystone \
    && cd /opt/keystone \
    && pip install -r /opt/keystone-requirements.txt \
    && pip install . \
    && cp -r etc /etc/keystone \
    && pip install python-openstackclient==5.2.1 \
    && pip install mysqlclient \
    && pip install python-memcached \
    && pip install uwsgi \
    && rm -rf /opt/keystone

COPY keystone.conf /etc/keystone/keystone.conf
COPY logging.conf /etc/keystone/logging.conf

COPY entrypoint.sh entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]
EXPOSE 5000
