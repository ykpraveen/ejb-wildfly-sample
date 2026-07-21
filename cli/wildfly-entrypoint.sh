#!/bin/bash
# WildFly entrypoint: installs MySQL JDBC driver module and configures datasource,
# then starts WildFly with standalone-full.xml.

set -euo pipefail

WILDFLY_HOME="/opt/jboss/wildfly"
JBOSS_CLI="$WILDFLY_HOME/bin/jboss-cli.sh"
MODULE_DIR="$WILDFLY_HOME/modules/com/mysql/mysql-connector-j/main"
DRIVER_JAR="$MODULE_DIR/mysql-connector-j.jar"
CONFIG_XML="$WILDFLY_HOME/standalone/configuration/standalone-full.xml"
CONFIG_BAK="$WILDFLY_HOME/standalone/configuration/standalone-full.xml.orig"

MYSQL_DRIVER_URL="https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar"

# ── Step 1: Download MySQL JDBC driver ───────────────────────────────────────

if [ ! -f "$DRIVER_JAR" ]; then
    echo "[entrypoint] Downloading MySQL JDBC driver..."
    mkdir -p "$MODULE_DIR"
    curl -sL -o "$DRIVER_JAR" "$MYSQL_DRIVER_URL"
    if [ ! -f "$DRIVER_JAR" ]; then
        echo "[entrypoint] ERROR: Failed to download MySQL JDBC driver."
        exit 1
    fi
    echo "[entrypoint] MySQL JDBC driver downloaded: $(ls -lh "$DRIVER_JAR" | awk '{print $5}')"
else
    echo "[entrypoint] MySQL JDBC driver already installed."
fi

# ── Step 2: Install WildFly module for MySQL JDBC driver ─────────────────────

if [ ! -f "$MODULE_DIR/module.xml" ]; then
    echo "[entrypoint] Installing WildFly module for MySQL JDBC driver..."
    cat > "$MODULE_DIR/module.xml" <<'MODULE_XML'
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="com.mysql.mysql-connector-j">
    <resources>
        <resource-root path="mysql-connector-j.jar"/>
    </resources>
    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
    </dependencies>
</module>
MODULE_XML
    echo "[entrypoint] WildFly module installed."
else
    echo "[entrypoint] WildFly module already installed."
fi

# ── Step 3: Backup original standalone-full.xml on first run ─────────────────

if [ ! -f "$CONFIG_BAK" ]; then
    echo "[entrypoint] Saving clean standalone-full.xml backup..."
    cp "$CONFIG_XML" "$CONFIG_BAK"
fi

# ── Step 4: Configure datasource using embed-server ──────────────────────────

if grep -q "UserMgmtDS" "$CONFIG_XML" 2>/dev/null; then
    echo "[entrypoint] Datasources already configured."
else
    echo "[entrypoint] Restoring clean standalone-full.xml before configuring..."
    cp "$CONFIG_BAK" "$CONFIG_XML"

    echo "[entrypoint] Configuring datasources via embed-server..."
    "$JBOSS_CLI" --file=/opt/jboss/wildfly/standalone/configuration/cli/configure-datasource-full.cli
    echo "[entrypoint] Datasource configuration complete."
fi

# ── Step 5: Clean stale deployment markers ───────────────────────────────────

DEPLOYMENTS_DIR="$WILDFLY_HOME/standalone/deployments"
echo "[entrypoint] Cleaning deployment markers..."
find "$DEPLOYMENTS_DIR" -maxdepth 1 -name "*.failed" -delete 2>/dev/null || true
find "$DEPLOYMENTS_DIR" -maxdepth 1 -name "*.isdeploying" -delete 2>/dev/null || true
find "$DEPLOYMENTS_DIR" -maxdepth 1 -name "*.undeploying" -delete 2>/dev/null || true
find "$DEPLOYMENTS_DIR" -maxdepth 1 -name "*.undeployed" -delete 2>/dev/null || true
find "$DEPLOYMENTS_DIR" -maxdepth 1 -name "*.deployed" -delete 2>/dev/null || true

# Ensure .dodeploy marker exists for any EAR/WAR files without one
for artifact in "$DEPLOYMENTS_DIR"/*.ear "$DEPLOYMENTS_DIR"/*.war; do
    [ -f "$artifact" ] || continue
    marker="${artifact}.dodeploy"
    if [ ! -f "$marker" ] && [ ! -f "${artifact}.deployed" ]; then
        echo "[entrypoint] Creating deployment marker for $(basename "$artifact")"
        touch "$marker"
    fi
done

# ── Step 6: Start WildFly ────────────────────────────────────────────────────

echo "[entrypoint] Starting WildFly in foreground..."
exec "$WILDFLY_HOME/bin/standalone.sh" -c standalone-full.xml -b 0.0.0.0 -bmanagement 0.0.0.0
