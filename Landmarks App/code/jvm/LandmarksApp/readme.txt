Para que o servidor funcione, é necessário utilizar uma conta de serviço Google com as seguintes permissões:
- Cloud Datastore Owner
- Pub/Sub Admin
- Storage Admin

Para correr o servidor, executar os seguintes comandos:

set GOOGLE_APPLICATION_CREDENTIALS=<conta de serviço>
java -jar <server.jar> 8000

8000 -> porto do servidor