#!/bin/bash
# Exit immediately if a command fails
set -e

# --- This script is run by Terraform after the JAR is uploaded ---

echo "Starting application setup..."

# Update packages and install Java
sudo yum update -y
sudo yum install -y java-21-amazon-corretto-headless

sudo alternatives --set java /usr/lib/jvm/java-21-amazon-corretto.*/bin/java

# Create a directory for the application
mkdir -p /opt/nba-daily-fantasy-backend
mv /tmp/demo-0.0.1-SNAPSHOT.jar /opt/nba-daily-fantasy-backend/app.jar

# Create a secure environment file for the systemd service
touch /etc/sysconfig/myapp.env
chmod 600 /etc/sysconfig/myapp.env

# List of all parameters to fetch
PARAMS=(
    "DATABASE_USER"
    "DATABASE_URL"
    "DISCORD_API_PUBLIC_KEY"
    "DISCORD_CLIENT_SECRET"
    "VITE_DISCORD_CLIENT_ID"
    "BOT_ACCESS_KEY"
    "DB_PASSWORD"
)

# Loop through and write each parameter to the environment file
for PARAM in "${PARAMS[@]}"; do
    VALUE=$(aws ssm get-parameter --name "$PARAM" --with-decryption --query "Parameter.Value" --output text --region us-east-1)
    echo "$PARAM=$VALUE" >> /etc/sysconfig/myapp.env
done

# Create a systemd service file
cat <<EOT > /etc/systemd/system/myapp.service
[Unit]
Description=NBA Daily Fantasy Backend
After=network.target

[Service]
User=ec2-user
WorkingDirectory=/opt/nba-daily-fantasy-backend
EnvironmentFile=/etc/sysconfig/myapp.env
ExecStart=java -jar app.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOT

# Reload, enable, and start the application service
systemctl daemon-reload
systemctl enable myapp.service
systemctl start myapp.service

echo "Application setup complete and service started."