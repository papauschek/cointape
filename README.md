# CoinTape
Bitcoin Transaction Fees Prediction

CoinTape analyses Blockchain transaction fees and displays current and predicted fees. 

![CoinTape](/public/images/screenshot.png?raw=true "Transaction Fees Overview")

Requirements for Deployment:
- PostgreSQL database 9.2 or higher
- Java SDK 7 or higher
- Apache, nginx or other web server as reverse proxy.

Requirements for Development:
- SBT (Simple Build Tool) - http://www.scala-sbt.org/download.html
- PostgreSQL database 9.2 or higher
- Java SDK 7 or higher
- Node for frontend development (React/JSX): https://nodejs.org

How to run locally:
- Create a **database on your local PostgreSQL instance named "cointape"**, and user login for "cointape" / "cointape". You may change these settings in application.conf
- Run a **local Bitcoin Core daemon** with the parameters `-txindex=1 -rest`. The REST interface is necessary for fetching data from the blockchain. The transaction index is necessary to fetch full data about all transactions. Read more about this here: https://en.bitcoin.it/wiki/Running_Bitcoin  
- Make sure Node is installed, and an empty `cointape` PostgreSQL database is created and the db server is running.
- Switch to the cointape root directory and run `sbt run` to start CoinTape with its local development webserver.
- Go to `http://localhost:9000` and wait for CoinTape to pull the latest blockchain data and start predicting. This may fake a few minutes.

How to deploy:
- run `sbt stage`

Messages:

"not enough blocks for prediction"
Still analysing the latest blocks from Bitcoin Core. This will take a couple of minutes until at least 12 blocks are reached,

"[error] application - Block update error: java.net.ConnectException: Connection
refused: no further information: localhost/127.0.0.1:8332"
Bitcoin Core is not running, or does not have REST interface enabled, or is still booting up (while indexing its database).