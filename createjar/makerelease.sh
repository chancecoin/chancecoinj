mkdir db_temp
mv release/resources/db/* db_temp/
zip -r release.zip release/
mv db_temp/* release/resources/db/
rmdir db_temp