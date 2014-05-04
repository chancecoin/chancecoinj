mkdir db_temp
mv release_rpc/resources/db/* db_temp/
zip -r release_rpc.zip release_rpc/
mv db_temp/* release_rpc/resources/db/
rmdir db_temp