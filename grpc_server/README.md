--------------------------------------------------------------------------------------------------------------------------------
**CREATE BASE DIRECTORIES**  
--------------------------------------------------------------------------------------------------------------------------------
`sudo mkdir -p /var/cs244b.p2p.dns/state`  
`sudo mkdir -p /var/cs244b.p2p.dns/ssl_certificates/server`  
`sudo mkdir -p /var/cs244b.p2p.dns/ssl_certificates/client`  
`sudo mkdir -p /var/cs244b.p2p.dns/ssl_certificates/trusted_contacts`  
`sudo mkdir -p /var/cs244b.p2p.dns/ssl_certificates/supported_clients_mutual_tls`  

--------------------------------------------------------------------------------------------------------------------------------
**STEPS TO CREATE SELF SIGNED CERTIFICATES**  
--------------------------------------------------------------------------------------------------------------------------------
1) CREATE CONF FILE FOR CERTICATE. 
`cd /var/cs244b.p2p.dns/ssl_certificates`  
`sudo vi ssl_req.conf`  
	[req]  
	distinguished_name = req_distinguished_name  
	x509_extensions = v3_req  
	prompt = no  
	[req_distinguished_name]  
	C = IN  
	ST = MH  
	L = PUNE  
	O = Stanford  
	OU = CS244B  
	CN = localhost  
	[v3_req]  
	keyUsage = keyEncipherment, dataEncipherment  
	extendedKeyUsage = serverAuth  
	subjectAltName = @alt_names  
	[alt_names]  
	email = abc@xyz.com  
	IP.1 = 127.0.0.1  
	IP.2 = 3.133.102.2 ***\-\-\> Change it to public static IP of your server***  

2) CREATE CERTICATE  
Run the below command inside ***server*** & ***client*** directories in */var/cs244b.p2p.dns/ssl_certificates*  
`sudo openssl req -newkey rsa:2048 -new -nodes -x509 -days 3650 -keyout key.pem -out cert.pem -config ~/git/cs244b/ssl_certs/ssl_req.conf -extensions 'v3_req'`  

3) READ CERTIFICATE  
`openssl x509 -noout -text -in cert.pem`  

--------------------------------------------------------------------------------------------------------------------------------
**BACKEND SERVER DETAILS**  
--------------------------------------------------------------------------------------------------------------------------------
1) Make a local copy of *server.op.config*, *peers.json* & *domain_lookup_db.json*.  
*/var/cs244b.p2p.dns/server***\_local***.op.config*  
*/var/cs244b.p2p.dns/peers***\_local***.json*  
*/var/cs244b.p2p.dns/domain\_lookup\_db***\_local***.json*  

2) The below properties can be tweaked from the config file.  
**dnsExpiryDays:** 7 *\-\-\> number of days before which a freshly fetched DNS info is valid*  
**maxHopCount:** 5 *\-\-\> max number of hops allowed for a p2p request originating from the server*  
**cacheEnabled:** true *\-\-\> if true, subsequent requests are served from cache if present*  
**dnsCacheCapacity:** 1000000 *\-\-\> max number of records in cache*  
**permissibleHostNameLength:** 255 *\-\-\> host names greater than this are not resolved*  
**dnsStateFileLocation:** "/var/cs244b.p2p.dns/state" *\-\-\> directory where dns cache are written to disk for recovery in case of failure/restarts*  
**secureConnection:** true *\-\-\> if true, client authenticates the server using TLS*  
**mutualTlsEnabled:** true *\-\-\> if true, the server also authenticates the client*  
**peers:** "/var/cs244b.p2p.dns/peers.json" *\-\-\> connection details (name:ip:port) for trusted peers*  
**domainMapping:** "/var/cs244b.p2p.dns/domain_lookup_db.json" *\-\-\> domain lookup information of hostnames/sub domains - whether it directly resolves to an IP or delegates the resolution to one of its trusted contacts*  
**sslCertBaseLocation:** "/var/cs244b.p2p.dns/ssl_certificates" *\-\-\> directory where all client, server, trusted contacts certificates are present. There are further 4 directories inside it.*  
    **./server** - *pk (key.pem) and self-signed certificate (cert.pem) of the server*  
    **./client** - *pk (key.pem) and self-signed certificate (cert.pem) of the local client*  
    **./trusted_contacts/`<peer name>`/cert.pem** - *certificates of each trusted peers inside their respective directories*  
    **./supported_clients_mutual_tls/`<client peer name>`/cert.pem** - *certificates of each client peers inside their respective directories - used in case of mutual TLS*  

3) Modify the ***peers_local.json*** & ***domain_lookup_db_local.json*** file as per your need.  

4) Modify the ***server_local.op.config*** file to point to above *peers_local.json* & *domain_lookup_db_local.json* files.  

5) Run the java server using the local copy of the server config file.  
`java -jar ~/git/cs244b/grpc_server/target/grpc_server-0.001-SNAPSHOT-jar-with-dependencies.jar -port 9000 -config /var/cs244b.p2p.dns/server_local.op.config &`  

6) Run the python client to connect to the java server.  
`python3 ~/git/cs244b/client/client/runner.py --port 20000 --backend_ip=127.0.0.1 --backend_port=9000 --root_cert /var/cs244b.p2p.dns/ssl_certificates/server/cert.pem --private_key /var/cs244b.p2p.dns/ssl_certificates/client/key.pem --client_cert /var/cs244b.p2p.dns/ssl_certificates/client/cert.pem &`  

7) The below certificates/keys are not needed by client in case the *secureConnection* is 'false' in server config.  
**root_cert:** *needed to authenticate the server using TLS*  
**private_key:** *needed for TLS*  
**client_cert:** *needed by the server for mutual TLS*  

--------------------------------------------------------------------------------------------------------------------------------