# API paralela con ActixWeb (Framework Rust), uso de notificaciones push para la app móvil anterior, uso de Docker y desplegada en la nube (AWS, GCP, la que usted considere).

En este ejercicio, desarrollarás un sistema backend utilizando ActixWeb que maneje la ejecución de tareas en paralelo, y que se comunique con su aplicación móvil mediante notificaciones push.
El backend estará dockerizado y será desplegado en una nube (AWS o Digital Ocean).
La aplicación móvil recibirá notificaciones push cuando se completen las tareas solicitadas a través del backend.

# Objetivos:

## Desarrollar la API en ActixWeb:

1. Implementar un backend en ActixWeb que procese tareas intensivas en paralelo.
2. Incluir endpoints para que la aplicación móvil envíe solicitudes de tareas y reciba actualizaciones sobre el progreso.

3. Integrar notificaciones push:
   Utilizar un servicio de notificaciones push, como Firebase Cloud Messaging (FCM) o OneSignal, para enviar notificaciones a la aplicación móvil cuando las tareas hayan sido completadas.

4. Aplicación móvil:
   La aplicación móvil (de la semana anterior) debe interactuar con el backend a través de una API REST o WebSocket, solicitando la ejecución de tareas y mostrando notificaciones push cuando se completen.

5. Dockerización:
   Contenerizar la aplicación backend utilizando Docker, creando una imagen que incluya todo el entorno necesario para ejecutar el sistema de tareas en ActixWeb.

6. Despliegue en la nube:
   Desplegar la aplicación en un servicio de nube como AWS (Elastic Beanstalk, ECS o EC2) o Digital Ocean (App Platform o Droplets).
   Configurar escalabilidad y balanceo de carga para manejar múltiples solicitudes concurrentes
