# How to install

## Install Bitcoin Core

```
sudo apt-add-repository ppa:bitcoin/bitcoin
sudo apt-get install bitcoin-qt bitcoind
adduser bitcoin --disabled-login
```

- Set password in bitcoin configuration file /home/bitcoin/.bitcoin/bitcoin.conf
```
rpcuser=bitcoinrpc
rpcpassword=testpassword
```
(replace "testpassword" with random password)

- Copy deployment/upstart-bitcoind-conf to /etc/init/bitcoind.conf
- Bitcoin core will now run automatically on system start.
- run `sudo start bitcoind` to start it the first time


## Install CoinTape build requirements

### Install Java 8 and SBT

```
sudo add-apt-repository ppa:webupd8team/java
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
sudo apt-get update
sudo apt-get install sbt
sudo apt-get install oracle-java8-installer
```

### Install Node
	
```
sudo apt-get install nodejs
sudo ln -s /usr/bin/nodejs /usr/bin/node
```

### Clone CoinTape Git Repository

``` 
sudo apt-get install git
cd /root
git clone https://github.com/papauschek/cointape
``` 

### Install PostgreSQL 

``` 
sudo echo "deb http://apt.postgresql.org/pub/repos/apt/ trusty-pgdg main" >sudo /etc/apt/sources.list.d/pgdg.list
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
sudo apt-get update
sudo apt-get install postgresql
``` 

- Configure cointape user (will only have local access, so password does not matter):
```
sudo -u postgres psql <<EOT
CREATE USER cointape WITH PASSWORD 'cointape';
CREATE DATABASE "cointape" WITH OWNER "cointape" ENCODING 'UTF8';
EOT
```



## Automated CoinTape Build and Local Deployment

- Generate a secret application config (not part of the repository): Copy `conf/application.conf` to `conf/application.secret.conf` and set the Bitcoin Core password to the secret password generated earlier.

```
sudo adduser cointape --disabled-login
cd cointape
sudo deployment/auto-deploy
```

- CoinTape now starts automatically. Updates can be done by running `sudo deployment/auto-deploy` again. This will download the new sources, build, deploy and restart the server.


## Configure domain and reverse proxy

### Install nginx

```
sudo apt-get install nginx
sudo cp deployment/nginx.conf /etc/nginx/sites-enabled/cointape
```
- Configure domains in /etc/nginx/sites-enabled/cointape as needed. Then reload nginx:
```
sudo /etc/init.d/nginx reload
```
- CoinTape should now be accessible from a browser.
