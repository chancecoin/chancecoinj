mv resources/templates resources/templates_temp
mv resources/templates_chancecoin.com resources/templates

rm -Rf chancecoin.com
mkdir chancecoin.com
cp -r resources/static/* chancecoin.com/
cp .htaccess chancecoin.com/
mkdir chancecoin.com/downloads
#cp build/* chancecoin.com/downloads
#cp resources/db/chancecoin-1.db chancecoin.com/downloads
#cp resources/db/chancecoin.h2.db chancecoin.com/downloads
wget -O chancecoin.com/index.html http://0.0.0.0:8080/
wget -O chancecoin.com/participate.html http://0.0.0.0:8080/participate
wget -O chancecoin.com/technical.html http://0.0.0.0:8080/technical
wget -O chancecoin.com/casino.html http://0.0.0.0:8080/casino
wget -O chancecoin.com/wallet.html http://0.0.0.0:8080/wallet
wget -O chancecoin.com/exchange.html http://0.0.0.0:8080/exchange
wget -O chancecoin.com/balances.html http://0.0.0.0:8080/balances

mv resources/templates resources/templates_chancecoin.com
mv resources/templates_temp resources/templates
