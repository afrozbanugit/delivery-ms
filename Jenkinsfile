pipeline {
    agent any
    environment{
	APP_DIR='/var/lib/jenkins/workspace/delivery-ms/'
	JAR_FILE='delivery-ms-0.0.1-SNAPSHOT.jar'
    IMAGE = 'aafrozbanu/delivery-ms'
    IMAGE_TAG = "build-${BUILD_NUMBER}"
    DOCKER_CREDENTIALS_ID = 'docker-registry-login-id'
    AWS_CREDENTIALS_ID='Aws-login-id'
    AWS_REGION='us-west-1'
    CLUSTER_NAME='proj1-eks-cluster'
    DB_HOST='my-mysql-db.chaayacay20v.us-west-1.rds.amazonaws.com'
	}
    options {
        buildDiscarder(logRotator(numToKeepStr: '5', daysToKeepStr: '7'))
    }

    stages {
         stage('Clean workspace'){
            steps{
               cleanWs() 
            }
        }
        stage('Cloning Git Repo') {
            steps {
                script{
                    sh 'git clone "https://github.com/afrozbanugit/delivery-ms.git"'
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                script {                    
                    sh 'docker build -t ${IMAGE}:${IMAGE_TAG} delivery-ms/.'
                    sh "docker tag ${IMAGE}:${IMAGE_TAG} ${IMAGE}:latest"
                }
            }
        }
        stage('Push Docker Image') {
            steps {
                script {                    
                    docker.withRegistry('https://index.docker.io/v1/', "${DOCKER_CREDENTIALS_ID}") {
                        docker.image("${IMAGE}:${IMAGE_TAG}").push()
                        docker.image("${IMAGE}:latest").push()
                    }
                }
            }
        }
        stage('Deploy to EKS') {
            steps {
                withCredentials([[
                    $class: 'AmazonWebServicesCredentialsBinding',
                    credentialsId: "${AWS_CREDENTIALS_ID}"
                ],
                usernamePassword(
                    credentialsId: 'rds-db-credentials',
                    usernameVariable: 'DB_USERNAME',
                    passwordVariable: 'DB_PASSWORD'
                 )
                ]) {
                    sh '''
                        aws eks --region $AWS_REGION update-kubeconfig --name $CLUSTER_NAME
                         # Create secret and configmap
                            kubectl create secret generic db-secret \
                            --from-literal=username=$DB_USERNAME \
                            --from-literal=password=$DB_PASSWORD \
                            --dry-run=client -o yaml | kubectl apply -f -

                            kubectl create configmap db-config \
                            --from-literal=host=$DB_HOST \
                            --dry-run=client -o yaml | kubectl apply -f -

                        # Deploy App
                        sed -i "s|image: .*|image: ${IMAGE}:${IMAGE_TAG}|" delivery-ms/k8s-manifests/deployment.yml
                        # Now deploy renamed image
                        kubectl apply -f delivery-ms/k8s-manifests/deployment.yml
                        kubectl apply -f delivery-ms/k8s-manifests/service.yml
                    '''
                }
            }
        }
    }
    post {
        success {
            echo "✅ Deployed ${IMAGE}:${IMAGE_TAG} to EKS"
        }
        failure {
            echo "❌ Deployment failed."
        }
    }
}
