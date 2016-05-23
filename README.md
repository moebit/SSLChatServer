# SSLChatServer

## Usage

- Create a keystore by using the below command in command prompt:
```
keytool -genkey -keystore chatterServerKeyStore -keyalg RSA
```
- Run serverCore.java
- Clients can connect to the server via using openssl or any terminal applications that support SSL
- The client's command for openssl is: 
```
openssl s_client -connect x.x.x.x:3000
```
which x.x.x.x should be substituted with server's ip address.

Enjoy!
