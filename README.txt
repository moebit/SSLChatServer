1. Create a keystore by using the below command in command prompt:

	keytool -genkey -keystore chatterServerKeyStore -keyalg RSA

2. Run serverCore.java

3. Clients can connect to the server via using openssl or any terminal applications that support SSL

4. The client's command for openssl is:
	
	openssl s_client -connect x.x.x.x:3000

which x should be substituted with server's ip address.

Enjoy!