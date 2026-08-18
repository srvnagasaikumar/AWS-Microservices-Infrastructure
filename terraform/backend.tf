terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  backend "s3" {
    bucket         = "devopswithaws-terraform-remote-state"
    key            = "myapp/prod/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "devopswithaws-state-locking"
    encrypt        = true
  }
}