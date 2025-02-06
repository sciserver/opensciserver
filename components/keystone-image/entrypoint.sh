#!/bin/bash
set -e

echo "=== Initializing Keystone ==="

MYSQL_HOST="${MYSQL_HOST:-mysql}"
MYSQL_DATABASE="${MYSQL_DATABASE:-keystone}"
MYSQL_USER="${MYSQL_USER:-keystone}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-test}"
MYSQL_SSL="${MYSQL_SSL:-FALSE}"

KS_ADMIN_TOKEN="${KS_ADMIN_TOKEN:-test}"
KS_ADMIN_PASSWORD="${KS_ADMIN_PASSWORD:-test}"
KS_SERVER_NAME=${KS_SERVER_NAME:-localhost}

OS_PASSWORD=$KS_ADMIN_PASSWORD
OS_USERNAME=admin
OS_PROJECT_NAME=admin
OS_USER_DOMAIN_NAME=Default
OS_PROJECT_DOMAIN_NAME=Default
OS_AUTH_URL=http://localhost:5000/v3
OS_IDENTITY_API_VERSION=3

echo "export OS_USERNAME=$OS_USERNAME
export OS_PASSWORD=$OS_PASSWORD
export OS_PROJECT_NAME=$OS_PROJECT_NAME
export OS_USER_DOMAIN_NAME=$OS_USER_DOMAIN_NAME
export OS_PROJECT_DOMAIN_NAME=$OS_PROJECT_DOMAIN_NAME
export OS_AUTH_URL=$OS_AUTH_URL
export OS_IDENTITY_API_VERSION=$OS_IDENTITY_API_VERSION" > /opt/admin-openrc.sh

WSGI_NUM_WORKERS="${WSGI_NUM_WORKERS:-4}"

mkdir -p /etc/keystone/fernet-keys/

sed -i \
    -e "s/KS_ADMIN_TOKEN/$KS_ADMIN_TOKEN/g" \
    -e "s/MYSQL_HOST/$MYSQL_HOST/g" \
    -e "s/MYSQL_DATABASE/$MYSQL_DATABASE/g" \
    -e "s/MYSQL_USER/$MYSQL_USER/g" \
    -e "s/MYSQL_PASSWORD/$MYSQL_PASSWORD/g" \
    -e "s/MYSQL_SSL/$MYSQL_SSL/g" \
/etc/keystone/keystone.conf

echo "---"
echo "Waiting for the database..."
while ! mysql --connect-timeout=5 -h"$MYSQL_HOST" -u $MYSQL_USER -p$MYSQL_PASSWORD --ssl=$MYSQL_SSL -e "SELECT 1" $MYSQL_DATABASE > /dev/null 2>&1; do
    sleep 5
done

echo "---"
echo "Setting up Fernet key repository..."
keystone-manage fernet_setup --keystone-user root --keystone-group root

echo "---"
echo "Initializing the database..."
keystone-manage db_sync

echo "---"
echo "Bootstrapping Keystone..."
keystone-manage bootstrap --bootstrap-password $KS_ADMIN_PASSWORD \
                --bootstrap-admin-url http://${KS_SERVER_NAME}:5000/v3/ \
                --bootstrap-internal-url http://${KS_SERVER_NAME}:5000/v3/ \
                --bootstrap-public-url http://${KS_SERVER_NAME}:5000/v3/ \
                --bootstrap-region-id RegionOne

echo "---"
echo "Starting WSGI server..."
uwsgi -p $WSGI_NUM_WORKERS --enable-threads --http 0.0.0.0:5000 --wsgi-file $(which keystone-wsgi-public) --disable-logging &

echo "---"
echo "Waiting for local keystone..."
while ! curl -f http://localhost:5000/v3/ > /dev/null 2>&1; do
    sleep 5
done

# we need to give a chance for kubernetes to check health and map the
# service to this pod, otherwise we might get a race condition in
# checks below, since we could hit them between termination of old pod
# and mapping of this one

echo "---"
echo "Waiting for Keystone service..."
sleep 5
while ! curl -f http://${KS_SERVER_NAME}:5000/v3/ > /dev/null 2>&1; do
    sleep 5
done

echo "---"
echo "Creating projects, users and roles..."
source /opt/admin-openrc.sh

echo "    checking for service project..."
if [[ -z $(openstack project list -f value -c Name | grep service) ]]; then
    echo "service project not found, creating..."
    openstack project create --domain default \
              --description "Service Project" service
fi

echo "    checking for demo project..."
if [[ -z $(openstack project list -f value -c Name | grep demo) ]]; then
    echo "demo project not found, creating..."
    openstack project create --domain default \
              --description "Demo Project" demo
fi

echo "    checking for demo user..."
if [[ -z $(openstack user list -f value -c Name | grep demo) ]]; then
    echo "demo user not found, creating"
    openstack user create --domain default \
              --password $KS_DEMO_PASSWORD demo
fi

echo "    checking for user role..."
if [[ -z $(openstack role list -f value -c Name | grep user) ]]; then
    echo "user role not found, creating..."
    openstack role create user
fi

echo "    adding role user to user demo in demo project..."
openstack role add --project demo --user demo user

if [[ -v KEYCLOAK_TRUST_PASS ]]; then
    echo "    checking for keycloaktrust project..."
    if [[ -z $(openstack project list -f value -c Name | grep keycloaktrust) ]]; then
        echo "keycloak project not found, creating..."
        openstack project create --domain default \
                  --description "Keycloak Project" keycloaktrust
    fi

    echo "    checking for keycloaktrust user..."
    if [[ -z $(openstack user list -f value -c Name | grep keycloaktrust) ]]; then
        echo "keycloaktrust user not found, creating"
        openstack user create --domain default \
                  --project keycloaktrust \
                  --password $KEYCLOAK_TRUST_PASS keycloaktrust
    fi

    echo "    adding role admin to user keycloaktrust in keycloaktrust project..."
    openstack role add --project keycloaktrust --user keycloaktrust admin
fi
if [[ -v CASJOBS_TRUST_PASS ]]; then
    echo "    checking for casjobs trust project"
    if [[ -z $(openstack project list -f value -c Name | grep $CASJOBS_TRUST_PROJECT) ]]; then
        echo "$CASJOBS_TRUST_PROJECT project not found, creating..."
        openstack project create --domain default \
                  --description "CasJobs Project" $CASJOBS_TRUST_PROJECT
    fi

    echo "    checking for casjobs trust user $CASJOBS_TRUST_USER..."
    if [[ -z $(openstack user list -f value -c Name | grep $CASJOBS_TRUST_USER) ]]; then
        echo "$CASJOBS_TRUST_USER user not found, creating"
        openstack user create --domain default \
                  --project $CASJOBS_TRUST_PROJECT \
                  --password $CASJOBS_TRUST_PASS $CASJOBS_TRUST_USER
    fi

    echo "    adding role admin to user $CASJOBS_TRUST_USER in $CASJOBS_TRUST_PROJECT project..."
    openstack role add --project $CASJOBS_TRUST_PROJECT --user $CASJOBS_TRUST_USER admin
fi

unset OS_PASSWORD OS_PROJECT_NAME OS_USER_DOMAIN_NAME OS_PROJECT_DOMAIN_NAME OS_AUTH_URL OS_IDENTITY_API_VERSION OS_USERNAME

echo "---"
echo "=== Done ==="
sleep infinity
