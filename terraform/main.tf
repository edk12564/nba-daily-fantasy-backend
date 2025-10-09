
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
    # FIX: Use a more standard Amazon Linux 2023 AMI name pattern
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
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
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = "t2.micro"
  key_name               = aws_key_pair.deployer_key.key_name
  vpc_security_group_ids = [aws_security_group.app_server_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_instance_profile.name

    # --- FIX: Added an explicit dependency to prevent an IAM race condition ---
  depends_on = [aws_iam_role_policy_attachment.attach_ssm_policy]

  # Connection block is required for provisioners
  connection {
    type        = "ssh"
    user        = "ec2-user"
    private_key = file(pathexpand("~/.ssh/id_rsa"))
    host        = self.public_ip
  }

  # Provisioner to upload your JAR file (runs first)
  provisioner "file" {
    source      = "../target/demo-0.0.1-SNAPSHOT.jar"
    destination = "/tmp/demo-0.0.1-SNAPSHOT.jar"
  }

   # Provisioner to upload the setup script
  provisioner "file" {
    source      = "setup.sh"
    destination = "/tmp/setup.sh"
  }

  # Provisioner to execute your setup script (runs second)
  provisioner "remote-exec" {
    inline = [
      "chmod +x /tmp/setup.sh",
      "sudo /tmp/setup.sh"
    ]
  }

  tags = {
    Name = "NBA Daily Fantasy Backend"
  }
}

output "application_url" {
  value = "http://${aws_instance.backend_server_1.public_ip}:8080"
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

# run everything in the script and run in the console

