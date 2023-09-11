Para que o lookup funcione, é necessário utilizar uma conta de serviço Google com as seguintes permissões:
- Compute Engine

Para correr o lookup executar os seguintes comandos:

gcloud functions deploy funcLookup --allow-unauthenticated --entry-point=functionhttp.Entrypoint --runtime=java11 --trigger-http --region=europe-west1 --source=target/deployment --service-account=<conta de serviço>