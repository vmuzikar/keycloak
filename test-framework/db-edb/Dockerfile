FROM registry.access.redhat.com/ubi9

# Get the token at https://www.enterprisedb.com/repos-downloads
ARG EDB_REPO_TOKEN=token-must-be-set
ENV VERSION=17
ENV PGUSER=enterprisedb
ENV PGPASSWORD=password
ENV PGDATABASE=keycloak
ENV PGPORT=5432

ENV PGDATA=/var/lib/edb/as${VERSION}/data

RUN (curl -1sSLf "https://downloads.enterprisedb.com/${EDB_REPO_TOKEN}/enterprise/setup.rpm.sh" | bash) && \
    dnf -y install edb-as${VERSION}-server

USER enterprisedb
WORKDIR /usr/edb/as${VERSION}/bin/

CMD pwfile=$(mktemp) && \
    echo "$PGPASSWORD" > "$pwfile" && \
    PGSETUP_INITDB_OPTIONS="-E UTF-8" ./initdb -A md5 -U $PGUSER --pwfile="$pwfile" && \
    echo "host all all 0.0.0.0/0 md5" >> "$PGDATA/pg_hba.conf"; \
    (while ! (sleep 1 && ./createdb $PGDATABASE > /dev/null 2>&1); do echo "Retrying database creation..."; done; echo "Database $PGDATABASE created.") & \
    exec ./edb-postgres -c port=$PGPORT -c logging_collector=off -c listen_addresses=*
EXPOSE ${PGPORT}