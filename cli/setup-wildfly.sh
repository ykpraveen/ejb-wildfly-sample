#!/bin/bash
# Configure WildFly datasource for Clinic application
# Prerequisites: WildFly running, MySQL connector JAR available
#
# Usage: ./setup-wildfly.sh [wildfly_home]
#
# Before running, place mysql-connector-j-8.3.0.jar in:
#   $WILDFLY_HOME/modules/com/mysql/mysql-connector-j/main/

set -euo pipefail

WILDFLY_HOME="${1:-/opt/wildfly}"
JBOSS_CLI="$WILDFLY_HOME/bin/jboss-cli.sh"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ ! -x "$JBOSS_CLI" ]; then
    echo "Error: jboss-cli.sh not found at $JBOSS_CLI"
    echo "Usage: $0 [wildfly_home]"
    exit 1
fi

echo "Configuring WildFly datasource..."
"$JBOSS_CLI" --file="$SCRIPT_DIR/configure-datasource.cli"
echo "Done. ClinicDS datasource configured."
