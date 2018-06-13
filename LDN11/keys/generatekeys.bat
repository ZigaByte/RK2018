keytool -genkey -alias serverprivate -keystore server.private -storetype JKS -keyalg rsa -dname "CN=localhost" -storepass password -keypass password -validity 365

keytool -genkey -alias alfaprivate -keystore alfa.private -storetype JKS -keyalg rsa -dname "CN=Alfa" -storepass password -keypass password -validity 365
keytool -genkey -alias betaprivate -keystore beta.private -storetype JKS -keyalg rsa -dname "CN=Beta" -storepass password -keypass password -validity 365
keytool -genkey -alias gamaprivate -keystore gama.private -storetype JKS -keyalg rsa -dname "CN=Gama" -storepass password -keypass password -validity 365

keytool -export -alias alfaprivate -keystore alfa.private -file temp.key -storepass password
keytool -import -noprompt -alias alfapublic -keystore clients.public -file temp.key -storepass password
del temp.key
keytool -export -alias betaprivate -keystore beta.private -file temp.key -storepass password
keytool -import -noprompt -alias betapublic -keystore clients.public -file temp.key -storepass password
del temp.key
keytool -export -alias gamaprivate -keystore gama.private -file temp.key -storepass password
keytool -import -noprompt -alias gamapublic -keystore clients.public -file temp.key -storepass password
del temp.key

keytool -export -alias serverprivate -keystore server.private -file temp.key -storepass password
keytool -import -noprompt -alias serverpublic -keystore server.public -file temp.key -storepass password
del temp.key
