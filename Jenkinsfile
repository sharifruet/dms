# Jenkins CI/CD Pipeline Configuration

pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'your-registry.com'
        IMAGE_TAG = "${BUILD_NUMBER}"
        BACKEND_IMAGE = "${DOCKER_REGISTRY}/dms-backend:${IMAGE_TAG}"
        FRONTEND_IMAGE = "${DOCKER_REGISTRY}/dms-frontend:${IMAGE_TAG}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Backend') {
            steps {
                dir('backend') {
                    sh 'chmod +x mvnw'
                    sh './mvnw clean package -DskipTests'
                }
            }
        }
        
        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }
        
        stage('Run Tests') {
            parallel {
                stage('Backend Tests') {
                    steps {
                        dir('backend') {
                            sh './mvnw test'
                        }
                    }
                }
                stage('Frontend Tests') {
                    steps {
                        dir('frontend') {
                            sh 'npm test -- --coverage --watchAll=false'
                        }
                    }
                }
            }
        }
        
        stage('Docker Build') {
            steps {
                script {
                    // Build backend image
                    sh "docker build -f backend/Dockerfile -t ${BACKEND_IMAGE} backend/"
                    
                    // Build frontend image
                    sh "docker build -f frontend/Dockerfile -t ${FRONTEND_IMAGE} frontend/"
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                script {
                    // Run Trivy security scan
                    sh "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image ${BACKEND_IMAGE}"
                    sh "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image ${FRONTEND_IMAGE}"
                }
            }
        }
        
        stage('Push Images') {
            when {
                branch 'main'
            }
            steps {
                script {
                    sh "docker push ${BACKEND_IMAGE}"
                    sh "docker push ${FRONTEND_IMAGE}"
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'main'
            }
            steps {
                script {
                    // Deploy to staging environment
                    sh "kubectl set image deployment/dms-backend dms-backend=${BACKEND_IMAGE} -n staging"
                    sh "kubectl set image deployment/dms-frontend dms-frontend=${FRONTEND_IMAGE} -n staging"
                }
            }
        }
        
        stage('Integration Tests') {
            when {
                branch 'main'
            }
            steps {
                script {
                    // Run integration tests against staging
                    sh "npm run test:integration"
                }
            }
        }
    }
    
    post {
        always {
            // Clean up workspace
            cleanWs()
        }
        success {
            // Send success notification
            emailext (
                subject: "Build Success: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Build completed successfully!",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
        failure {
            // Send failure notification
            emailext (
                subject: "Build Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: "Build failed. Please check the logs.",
                to: "${env.CHANGE_AUTHOR_EMAIL}"
            )
        }
    }
}
