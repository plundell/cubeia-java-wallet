# Wallet REST API Demo

This Java 21 Sprint Boot REST API allows a user or system to:
- Create a wallet
- See current balance
- List transactions
- Transfer "money" to other wallets

**Design Features:** 
- Stateless 
- Horizontally scaleable 

**API Endpoints:**
| Method | URL                        | Description                           |
|--------|----------------------------|---------------------------------------|
| PUT    | /api/wallet                | Create a new wallet                   |
| GET    | /api/balance     	      | Get the current balance of a wallet   |
| GET    | /api/transactions          | List all transactions for wallet      |
| POST   | /api/transfer          	  | Transfer money to another wallet	  |



## Development

To develop or test this application locally you should download the sources from github, then build using Maven 3.8+ and run using a Java 21 Runtime Enviornment

```bash
git clone https://github.com/your-username/demo-api.git
cd demo-api
# make andy changes you like in the source code files...
mvn test
# make some more changes...
mvn clean package
java -jar target/demo-api-0.0.1-SNAPSHOT.jar
# now the application is listening on whichever ports you've configured. 8080 by default.
```
Now you can open a browser to http://localhost:8080/api/ to confirm it's working. To test specific endpoints you can use something like Postman and make requests like:

```json
{
  ...
}
```


## Deployment

For deployment we build a docker container which can be pushed to a suitable cloud service. 

Your first port of call is to copy the jar file you created above into the container and try to run it 
```bash
docker-compose up -d  #after this it will be running
docker-compose ps  #this will list your container as running
docker-compose stop #this will stop it
```
This will use docker-compose.yml and the target copy_local in Dockerfile.

When the above works you can build inside a docker container which is what will happen when a CI pipeline is setup. 
```bash
docker build --target in_container -t api-container .
```



## License

MIT 