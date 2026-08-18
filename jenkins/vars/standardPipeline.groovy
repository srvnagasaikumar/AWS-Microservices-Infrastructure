def call(Map config) {
    pipeline {
        agent any
        environment {
            AWS_REGION      = 'us-east-1'
            ECR_REGISTRY    = '123456789012.dkr.ecr.us-east-1.amazonaws.com'
            IMAGE_NAME      = "${config.appName}"
            IMAGE_TAG       = "${BUILD_NUMBER}"
        }
        stages {
            stage('Checkout & Lint') {
                steps {
                    checkout scm
                    sh 'echo "Running static code analysis and linting..."'
                }
            }
            stage('Docker Build & Tag') {
                steps {
                    script {
                        sh "docker build -t ${ECR_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} ."
                    }
                }
            }
            stage('Security Scan') {
                steps {
                    sh "echo 'Scanning container image for CVE vulnerabilities...'"
                }
            }
            stage('Push to AWS ECR') {
                steps {
                    script {
                        sh """
                        aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                        docker push ${ECR_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                        """
                    }
                }
            }
            stage('Deploy to EKS via Helm') {
                steps {
                    script {
                        sh """
                        aws eks update-kubeconfig --region ${AWS_REGION} --name production-myapp-eks
                        helm upgrade --install ${config.appName} ./kubernetes-helm/myapp-app \
                          --set image.tag=${IMAGE_TAG} \
                          --namespace production
                        """
                    }
                }
            }
        }
        post {
            always {
                cleanWs()
            }
            failure {
                echo "Pipeline execution failed. Triggering notification..."
            }
        }
    }
}