terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}

# Find the latest Amazon Linux AMI automatically
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }
}

# REQUIRED: A security group to open ports for your app and for SSH
resource "aws_security_group" "app_server_sg" {
  name = "nba-app-sg"
  # Allow inbound traffic on port 8080 for your Spring Boot app
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  # Allow inbound traffic on port 22 for SSH
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # For security, you can restrict this to your IP
  }
  # Allow all outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# REQUIRED: An SSH key to allow access to the instance
resource "aws_key_pair" "deployer_key" {
  key_name   = "nba-app-key"
  # Use pathexpand to correctly resolve the home directory path
  public_key = file(pathexpand("~/.ssh/id_rsa.pub"))
}

resource "aws_instance" "backend_server_1" {
  ami           = data.aws_ami.amazon_linux.id
  instance_type = "t2.micro"
  key_name      = aws_key_pair.deployer_key.key_name
  vpc_security_group_ids = [aws_security_group.app_server_sg.id]

  user_data = <<-EOF
              #!/bin/bash
              yum update -y
              yum install -y java-21-amazon-corretto-devel

              # Create a directory for our application
              mkdir -p /opt/nba-daily-fantasy-backend

              # Move the uploaded JAR file to its final destination
              mv /tmp/demo-0.0.1-SNAPSHOT.jar /opt/nba-daily-fantasy-backend/app.jar

              # Create a systemd service file to manage the application
              cat <<EOT > /etc/systemd/system/myapp.service
              [Unit]
              Description=NBA Daily Fantasy Backend
              After=network.target

              [Service]
              User=ec2-user
              # FIXED: Corrected path to the JAR file
              ExecStart=/usr/bin/java -jar /opt/nba-daily-fantasy-backend/app.jar
              Restart=on-failure
              RestartSec=10

              [Install]
              WantedBy=multi-user.target
              EOT

              # Reload, enable, and start the application service
              systemctl daemon-reload
              systemctl enable myapp.service
              systemctl start myapp.service
              EOF

  # REQUIRED: Connection block for the file provisioner
  connection {
    type        = "ssh"
    user        = "ec2-user"
    # FIXED: Added pathexpand to correctly find your private key
    private_key = file(pathexpand("~/.ssh/id_rsa"))
    host        = self.public_ip
  }

  # REQUIRED: Provisioner to upload your JAR file
  provisioner "file" {
    source      = "demo-0.0.1-SNAPSHOT.jar"
    destination = "/tmp/demo-0.0.1-SNAPSHOT.jar"
  }

  tags = {
    Name = "NBA Daily Fantasy Backend"
  }
}

output "application_url" {
  value = "http://${aws_instance.backend_server_1.public_ip}:8080"
}

# ip, route 53 connection, check if route 53 is free



