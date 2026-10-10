#!/bin/bash
# Idempotent post-upload setup for the EC2 backend (run by Terraform).
set -euo pipefail

APP_DIR=/opt/nba-daily-fantasy-backend
JAR_SRC=/tmp/demo-0.0.1-SNAPSHOT.jar
ENV_FILE=/etc/sysconfig/myapp.env
DOMAIN=picknrolls.click
LE_LIVE=/etc/letsencrypt/live/${DOMAIN}
REGION=us-east-1
CERTBOT_EMAIL=alderwoodsolutionsllc@gmail.com

echo "Starting application setup..."

# --- Java 21 (install only if missing) ---
if ! java -version 2>&1 | grep -q '21\.'; then
  yum install -y java-21-amazon-corretto-headless
fi
JAVA_BIN=$(readlink -f "$(command -v java)" 2>/dev/null || true)
if [[ -n "${JAVA_BIN}" ]]; then
  alternatives --set java "${JAVA_BIN}" 2>/dev/null || true
fi

# --- App directory + JAR (do not wipe PEMs / other files) ---
mkdir -p "${APP_DIR}"
mv "${JAR_SRC}" "${APP_DIR}/app.jar"

# --- SSM → EnvironmentFile ---
PARAMS=(
  DATABASE_USER
  DATABASE_URL
  DISCORD_API_PUBLIC_KEY
  DISCORD_CLIENT_SECRET
  VITE_DISCORD_CLIENT_ID
  BOT_ACCESS_KEY
  DB_PASSWORD
  JWT_SECRET
)

TMP_ENV=$(mktemp)
chmod 600 "${TMP_ENV}"
for PARAM in "${PARAMS[@]}"; do
  VALUE=$(aws ssm get-parameter \
    --name "${PARAM}" \
    --with-decryption \
    --query 'Parameter.Value' \
    --output text \
    --region "${REGION}")
  printf '%s=%s\n' "${PARAM}" "${VALUE}" >> "${TMP_ENV}"
done
install -o root -g root -m 600 "${TMP_ENV}" "${ENV_FILE}"
rm -f "${TMP_ENV}"

# --- Certbot (install once) ---
# AL2023 system python3 is 3.9 for the life of the OS. Certbot 5+ drops 3.9, so the
# pip venv must use python3.11+ (do not point /usr/bin/python3 at a newer version).
ensure_certbot() {
  if command -v certbot >/dev/null 2>&1 \
      && [[ "$(readlink -f "$(command -v certbot)" 2>/dev/null || true)" != /opt/certbot/bin/certbot ]]; then
    # Distro package (or other non-venv install) is fine.
    return 0
  fi

  if [[ ! -x /opt/certbot/bin/certbot ]]; then
    if yum install -y certbot 2>/dev/null && command -v certbot >/dev/null 2>&1; then
      return 0
    fi
  fi

  # Rebuild the venv if missing or still on Python 3.9.
  local py_ver=""
  if [[ -x /opt/certbot/bin/python ]]; then
    py_ver=$(/opt/certbot/bin/python -c 'import sys; print(f"{sys.version_info.major}.{sys.version_info.minor}")')
  fi
  if [[ ! -x /opt/certbot/bin/certbot || "${py_ver}" == "3.9" ]]; then
    yum install -y python3.11 python3.11-pip
    rm -rf /opt/certbot
    python3.11 -m venv /opt/certbot
    /opt/certbot/bin/pip install --upgrade pip
    /opt/certbot/bin/pip install certbot
  fi
  ln -sfn /opt/certbot/bin/certbot /usr/local/bin/certbot
}

sync_certs() {
  cp -L "${LE_LIVE}/fullchain.pem" "${APP_DIR}/fullchain.pem"
  cp -L "${LE_LIVE}/privkey.pem" "${APP_DIR}/privkey.pem"
  chown ec2-user:ec2-user "${APP_DIR}/fullchain.pem" "${APP_DIR}/privkey.pem"
  chmod 644 "${APP_DIR}/fullchain.pem"
  chmod 600 "${APP_DIR}/privkey.pem"
}

ensure_certbot

if [[ ! -f "${LE_LIVE}/fullchain.pem" ]]; then
  systemctl stop nba.service 2>/dev/null || true
  certbot certonly \
    --standalone \
    --non-interactive \
    --preferred-challenges http \
    --agree-tos \
    -m "${CERTBOT_EMAIL}" \
    -d "${DOMAIN}"
fi

sync_certs

# Idempotent renew schedule (standalone on :80; app listens on 8443).
# Amazon Linux often has no /etc/cron.d until cronie is installed.
yum install -y cronie
mkdir -p /etc/cron.d
systemctl enable --now crond.service

cat >/etc/cron.d/nba-certbot <<EOF
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/opt/certbot/bin
0 0,12 * * * root sleep \$((RANDOM % 3600)); certbot renew -q --deploy-hook 'cp -L ${LE_LIVE}/fullchain.pem ${APP_DIR}/fullchain.pem && cp -L ${LE_LIVE}/privkey.pem ${APP_DIR}/privkey.pem && chown ec2-user:ec2-user ${APP_DIR}/fullchain.pem ${APP_DIR}/privkey.pem && chmod 644 ${APP_DIR}/fullchain.pem && chmod 600 ${APP_DIR}/privkey.pem && systemctl restart nba.service'
EOF
chmod 644 /etc/cron.d/nba-certbot

chown -R ec2-user:ec2-user "${APP_DIR}"
# Re-apply key perms after recursive chown
chmod 600 "${APP_DIR}/privkey.pem"
chmod 644 "${APP_DIR}/fullchain.pem"
chmod 644 "${APP_DIR}/app.jar"

# --- systemd ---
cat >/etc/systemd/system/nba.service <<EOF
[Unit]
Description=NBA Daily Fantasy Backend
After=network.target

[Service]
User=ec2-user
WorkingDirectory=${APP_DIR}
EnvironmentFile=${ENV_FILE}
ExecStart=java -jar app.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable nba.service
systemctl restart nba.service

echo "Application setup complete and service started."
