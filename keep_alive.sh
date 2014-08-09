ps -ef | grep -v grep | grep Server
if [ $? -eq 1 ]
then
cd /var/www/chancecoin.com/chancecoinj
nohup sh start_server.sh &
else
echo "Already running"
fi
