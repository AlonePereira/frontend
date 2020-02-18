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
        def IMAGE_POSTFIX = ""
        def ENVIRONMENT
        def GIT_REPOS_URL = "git@github.com:AlonePereira/frontend.git"
        def GIT_BRANCH
        def KUBE_NAMESPACE
        def CHARTMUSEUM_URL = "http://helm-chartmuseum:8080"
        def HELM_DEPLOY_NAME
        def HELM_CHART_NAME = "questcode/frontend"
        def NODE_PORT = "30080"

        stage('Checkout') {
            REPOS = checkout([$class: 'GitSCM', branches: [[name: "*/master" ]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'github', url: GIT_REPOS_URL]]])
            GIT_BRANCH = REPOS.GIT_BRANCH
            
            if (GIT_BRANCH.equals("origin/master")) {
                KUBE_NAMESPACE = "prod"
                ENVIRONMENT = "production"
            } else if (GIT_BRANCH.equals("origin/develop")) {
                KUBE_NAMESPACE = "staging"
                ENVIRONMENT = "staging"
                IMAGE_POSTFIX = "-RC"
                NODE_PORT = "31080"
            } else {
                def error = "Não existe pipeline para a branch ${GIT_BRANCH}"
                echo error
                throw new Exception(error)
            }

            HELM_DEPLOY_NAME = KUBE_NAMESPACE + "-frontend"
            IMAGE_VERSION = sh returnStdout: true, script: 'sh read-package-version.sh'
            IMAGE_VERSION = IMAGE_VERSION.trim() + IMAGE_POSTFIX
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
                """
                try {
                    sh "helm upgrade ${HELM_DEPLOY_NAME} ${HELM_CHART_NAME} --namespace ${KUBE_NAMESPACE} --set image.tag=${IMAGE_VERSION} --set service.nodePort=${NODE_PORT}"
                } catch (Exception e) {
                    sh "helm install ${HELM_DEPLOY_NAME} ${HELM_CHART_NAME} --namespace ${KUBE_NAMESPACE} --set image.tag=${IMAGE_VERSION} --set service.nodePort=${NODE_PORT}"
                }
            }
        }
    }
}