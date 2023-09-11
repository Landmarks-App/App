
java -jar <client.jar> 8000

8000 -> porto do servidor gRPC

Os parâmetros do pedido HTTP feito ao Lookup Function estão "hardcoded" na função getAvailableServers() da classe ClientApp.
Para que funcione com outro projeto e outro instance group, é preciso alterar o pedido presente nessa função.