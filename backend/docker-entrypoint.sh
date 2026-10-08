#!/bin/sh
set -eu
umask 077

fail() { echo "$1" >&2; exit 1; }
[ "${SPRING_PROFILES_ACTIVE:-}" = "prod,render" ] || fail 'Use SPRING_PROFILES_ACTIVE=prod,render.'
[ -n "${PREMIERHUB_DB_USER:-}" ] && [ -n "${PREMIERHUB_DB_PASSWORD:-}" ] || fail 'Configure the Aiven database credentials.'
case "${PREMIERHUB_JDBC_URL:-}" in
    *\?*|*\#*|*@*) fail 'Use a MySQL JDBC URL without query parameters, fragments or embedded credentials. TLS and UTC are configured by the render profile.' ;;
    jdbc:mysql://*/*) ;;
    *) fail 'Configure PREMIERHUB_JDBC_URL=jdbc:mysql://AIVEN_HOST:AIVEN_PORT/DATABASE.' ;;
esac

ca_file="${PREMIERHUB_AIVEN_CA_FILE:-/etc/secrets/aiven-ca.pem}"
[ -r "$ca_file" ] || fail 'The Aiven CA secret file is missing or unreadable.'
tls_dir=$(mktemp -d /tmp/prismaxi-tls.XXXXXX)
# Import every CA in the bundle, including old/new CAs during Aiven certificate rotation.
awk -v dir="$tls_dir" '
    /-----BEGIN CERTIFICATE-----/ {
        if (inside) exit 1
        count++; inside=1; file=dir "/ca-" count ".pem"
    }
    inside { sub(/\r$/, ""); print > file }
    /-----END CERTIFICATE-----/ { close(file); inside=0 }
    END { if (inside || count == 0) exit 1 }
' "$ca_file" || fail 'The Aiven CA bundle is not valid PEM.'
for certificate in "$tls_dir"/ca-*.pem; do
    keytool -importcert -noprompt -alias "$(basename "$certificate" .pem)" \
        -file "$certificate" -keystore "$tls_dir/truststore.p12" \
        -storetype PKCS12 -storepass changeit >/dev/null 2>&1 \
        || fail 'Cannot import the Aiven CA; refusing to start without certificate verification.'
done
# This truststore contains public CA certificates only; changeit is not a database credential.
export PREMIERHUB_DB_TRUSTSTORE_URL="file:$tls_dir/truststore.p12"
exec java -jar /app/app.jar "$@"
