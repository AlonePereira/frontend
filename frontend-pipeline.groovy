podTemplate(
    label: 'questcode', 
    containers: [
        containerTemplate(args: 'cat', name: 'docker', command: '/bin/sh -c', image: 'docker', ttyEnabled: true)
    ],
    volumes: [
      hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
    ]
)
{
    node('questcode') {
        def repos
        stage('Checkout') {
            repos = git branch: 'develop', credentialsId: 'github', url: 'git@github.com:AlonePereira/frontend.git'
        }
        stage('Package') {
            container('docker') {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', passwordVariable: 'DOCKER_HUB_PASSWORD', usernameVariable: 'DOCKER_HUB_USER')]) {
                    sh "docker login -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD}"
                    sh "docker build -t alonesilva/frontend:0.1.4 . --build-arg NPM_ENV='staging'"
                    sh 'docker push alonesilva/frontend:0.1.4'
                }
            }
        }
        stage('Deploy') {
            echo 'Iniciando Deploy com Helm'
        }
    }
}