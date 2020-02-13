podTemplate(
    label: 'questcode', 
    containers: [
        containerTemplate(args: 'cat', name: 'docker-container', command: '/bin/sh -c', image: 'docker', ttyEnabled: true),
        containerTemplate(args: 'cat', name: 'helm-container', command: '/bin/sh -c', image: 'lachlanevenson/k8s-helm:v3.0.2', ttyEnabled: true)
    ],
    volumes: [
      hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
    ]
)
{
    node('questcode') {
        def REPOS
        def IMAGE_VERSION
        def IMAGE_NAME = "frontend"
        def ENVIRONMENT = "staging"
        def GIT_REPOS_URL = "git@github.com:AlonePereira/frontend.git"
        def CHARTMUSEUM_URL = "http://helm-chartmuseum:8080"

        stage('Checkout') {
            REPOS = git branch: 'develop', credentialsId: 'github', url: GIT_REPOS_URL
            IMAGE_VERSION = sh returnStdout: true, script: 'sh read-package-version.sh'
            IMAGE_VERSION = IMAGE_VERSION.trim()
        }
        stage('Package') {
            container('docker-container') {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', passwordVariable: 'DOCKER_HUB_PASSWORD', usernameVariable: 'DOCKER_HUB_USER')]) {
                    sh "docker login -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD}"
                    sh "docker build -t ${DOCKER_HUB_USER}/${IMAGE_NAME}:${IMAGE_VERSION} . --build-arg NPM_ENV='${ENVIRONMENT}'"
                    sh "docker push ${DOCKER_HUB_USER}/${IMAGE_NAME}:${IMAGE_VERSION}"
                }
            }
        }
        stage('Deploy') {
            container('helm-container') {
                echo 'Iniciando Deploy com Helm'

                sh """
                    helm repo add questcode ${CHARTMUSEUM_URL}
                    helm repo update
                    helm upgrade staging-frontend questcode/frontend --namespace staging --set image.tag=${IMAGE_VERSION}
                """
            }
        }
    }
}