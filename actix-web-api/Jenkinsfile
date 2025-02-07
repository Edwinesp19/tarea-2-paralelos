pipeline {
    agent any

    stages {
        stage('Descargar Código') {
            steps {
                git branch: 'main', url: 'https://github.com/Edwinesp19/tarea-2-paralelos'
            }
        }

        stage('Construir y Probar') {
            steps {
                bat 'echo "Compilando código..."'
                bat 'echo "Ejecutando pruebas..."'
            }
        }
stage('Desplegar en Servidor') {
            when {
                branch 'main'
            }
            steps {
                bat 'echo "Desplegando en el servidor..."'
                // Aquí puedes agregar comandos para desplegar la imagen Docker en tu servidor
            }
        }
    }
}