cd createjar
sh packager.sh
sh makerelease.sh
cd ../
cp createjar/release.zip build/Chancecoin-2.2.zip

#to do a release:
# 1. Change the version number in the Config.java file.
# 2. Change the version number on line 5 above.
# 3. Run sh generate_package.sh
# 4. Change download.txt and min_version.txt
# 5. sh generate_chancecoin.com.sh; sh generate_downloads.sh; scp -r chancecoin.com chancecoin@chancecoin.com:~
# 6. git commit -am "update"; git push
