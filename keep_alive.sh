ps -ef | grep -v grep | grep Server
if [ $? -eq 1 ]
then
nohup sh /var/www/chancecoin.com/chancecoinj/start_server.sh &
else
echo "Already running"
fi
