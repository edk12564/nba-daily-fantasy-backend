
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}

# 1. Create the DNS Hosted Zone for the domain
resource "aws_route53_zone" "primary" {
  name = "picknrolls.click"
}

# Describes the existing A record
resource "aws_route53_record" "www_record" {
  zone_id = aws_route53_zone.primary.zone_id
  name    = "picknrolls.click"
  type    = "A"
  ttl     = 300
  records = ["${aws_instance.backend_server_1.public_ip}"]
}

# 2. Register the domain name and associate it with the hosted zone's name servers
resource "aws_route53domains_registered_domain" "registration" {
  domain_name = aws_route53_zone.primary.name

  # Pass the name servers from the created hosted zone to the domain registration
  name_server {
    name = aws_route53_zone.primary.name_servers[0]
  }
  name_server {
    name = aws_route53_zone.primary.name_servers[1]
  }
  name_server {
    name = aws_route53_zone.primary.name_servers[2]
  }
  name_server {
    name = aws_route53_zone.primary.name_servers[3]
  }

  # Auto-renew the domain registration
  auto_renew = true
}

# Find the latest Amazon Linux AMI automatically
# data "aws_ami" "amazon_linux" {
#   owners      = ["amazon"]
#
#   filter {
#     name   = "name"
#     # FIX: Use a more standard Amazon Linux 2023 AMI name pattern
#     values = ["al2023-ami-2023.*-x86_64"]
#   }
#
#   filter {
#     name   = "architecture"
#     values = ["x86_64"]
#   }
# }

# REQUIRED: A security group to open ports for your app and for SSH
resource "aws_security_group" "app_server_sg" {
  name = "nba-app-sg"
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  # Allow inbound traffic on port 80 for the Certbot/Let's Encrypt challenge
  ingress {
    from_port   = 8443
    to_port     = 8443
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
  # ami                    = data.aws_ami.amazon_linux.id
  ami = "ami-080c353f4798a202f"
  instance_type          = "t2.micro"
  key_name               = aws_key_pair.deployer_key.key_name
  vpc_security_group_ids = [aws_security_group.app_server_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_instance_profile.name

    # --- FIX: Added an explicit dependency to prevent an IAM race condition ---
  depends_on = [aws_iam_role_policy_attachment.attach_ssm_policy]

  tags = {
    Name = "NBA Daily Fantasy Backend test"

  }
}

# This resource runs a provisioner AFTER the instance and Route 53 domain are ready.
resource "null_resource" "setup_instance" {

  # This makes the null_resource wait for the domain registration to finish.
  depends_on = [aws_route53domains_registered_domain.registration]

  # Connection details for the EC2 instance we want to provision
  connection {
    type        = "ssh"
    user        = "ec2-user"
    private_key = file(pathexpand("~/.ssh/id_rsa"))
    host        = aws_instance.backend_server_1.public_ip
  }

  # Provisioner to upload your JAR file
  provisioner "file" {
    source      = "../target/demo-0.0.1-SNAPSHOT.jar"
    destination = "/tmp/demo-0.0.1-SNAPSHOT.jar"
  }

  # Provisioner to upload the setup script
  provisioner "file" {
    source      = "setup.sh"
    destination = "/tmp/setup.sh"
  }

  # Provisioner to execute your setup script
  provisioner "remote-exec" {
    inline = [
      "chmod +x /tmp/setup.sh",
      "sudo /tmp/setup.sh"
    ]
  }
}

resource "aws_iam_role" "ec2_role" {
  name = "ec2-parameter-store-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_policy" "parameter_store_read" {
  name        = "parameter-store-read-policy"
  description = "Allow EC2 instances to read from SSM Parameter Store"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = [
          "ssm:GetParameter",
          "ssm:GetParameters",
          "ssm:GetParametersByPath"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "attach_ssm_policy" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = aws_iam_policy.parameter_store_read.arn
}

resource "aws_iam_instance_profile" "ec2_instance_profile" {
  name = "ec2-parameter-store-profile"
  role = aws_iam_role.ec2_role.name
}


