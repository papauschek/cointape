
```
sudo apt-add-repository ppa:bitcoin/bitcoin
sudo apt-get install bitcoin-qt bitcoind
adduser bitcoin --disabled-login

```

Set password in bitcoin configuration file
/home/bitcoin/.bitcoin/bitcoin.conf
```
rpcuser=bitcoinrpc
rpcpassword=testpassword
```
(replace "testpassword" with random password)

