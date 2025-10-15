#!/bin/bash

# Exit immediately if a command fails
set -e

# --- This script is run by Terraform after the JAR is uploaded ---
sudo echo "Starting application setup..."

# Update packages and install Java
sudo yum update -y
sudo yum install -y java-21-amazon-corretto-headless

sudo alternatives --set java /usr/lib/jvm/java-21-amazon-corretto.*/bin/java

# Create a directory for the application
sudo mkdir -p /opt/nba-daily-fantasy-backend
sudo mv /tmp/demo-0.0.1-SNAPSHOT.jar /opt/nba-daily-fantasy-backend/app.jar

# Create a secure environment file for the systemd service
sudo touch /etc/sysconfig/myapp.env
sudo chmod 600 /etc/sysconfig/myapp.env

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
    sudo echo "$PARAM=$VALUE" >> /etc/sysconfig/myapp.env
done

echo "nonsense"

# create tls cert and move to project directory and give it permissions
#sudo python3 -m venv /opt/certbot/
#sudo /opt/certbot/bin/pip install --upgrade pip
#sudo /opt/certbot/bin/pip install certbot
#sudo ln -s /opt/certbot/bin/certbot /usr/bin/certbot
#sudo certbot certonly --test-cert --standalone --non-interactive --preferred-challenges http --agree-tos -m alderwoodsolutionsllc@gmail.com -d picknrolls.click
#sudo cp /etc/letsencrypt/live/picknrolls.click/fullchain.pem /opt/nba-daily-fantasy-backend/fullchain.pem
#sudo cp /etc/letsencrypt/live/picknrolls.click/privkey.pem /opt/nba-daily-fantasy-backend/privkey.pem
cd /opt/nba-daily-fantasy-backend
sudo chown -R ec2-user:ec2-user /opt/nba-daily-fantasy-backend
sudo chmod 644 /opt/nba-daily-fantasy-backend/*.pem

# auto renew cert
echo "0 0,12 * * * root /opt/certbot/bin/python -c 'import random; import time; time.sleep(random.random() * 3600)' && sudo certbot renew -q" | sudo tee -a /etc/crontab > /dev/null

# Create a systemd service file
cat <<EOT > /etc/systemd/system/myapp.service
[Unit]
Description=NBA Daily Fantasy Backend
After=network.target

[Service]
User=ec2-user
WorkingDirectory=/opt/nba-daily-fantasy-backend
EnvironmentFile=/etc/sysconfig/myapp.env
ExecStart=java -jar app.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOT

# Reload, enable, and start the application service
sudo systemctl daemon-reload
sudo systemctl enable myapp.service
sudo systemctl start myapp.service

echo "Application setup complete and service started."