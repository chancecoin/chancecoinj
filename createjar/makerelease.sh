rm release.zip
cd release
zip -r release.zip * -x "*.DS_Store"
cp release.zip ../
cd ../
