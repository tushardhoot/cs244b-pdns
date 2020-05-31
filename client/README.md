So the runner.py will be the 'local server' that the client pings which will then
interact with a client for our P2PDNS Service, which will resolve that and then
our runner.py will resolve the browser / user's request.

# Set up
1. Install Python3
2. Run `pip3 install -r requirements.txt` to install all of the dependencies.

# To use your client to resolve traffic on your machine
Follow the instructions [here](https://developers.google.com/speed/public-dns/docs/using#change_your_dns_servers_settings)

# TO RUN UNIT TESTS
Use the following command to run all unit tests:
`python3 -m unittest discover -v`

All of the unit tests will be in the `test/` directory.

# TO RUN
For an insecure channel:
1. Launch your test server / backend (either ./simpe\_server.py or the javabackend)
2. Run the runner.py here
3. Send a request to the UDP port on localhost specifed that runner ran with (53 by default) -- I've serialized the request of a DNS query for 'walmart.com' if you want to test it with a real DNS query.

For a secure channel:
1. Run the Backend Server
2. Runner the runner using `python3 client/runner.py --root_cert cert.pem --private_key key.pem`.
Here the private\_key is the private key for the client and the root cert is
the backend's certificate.
3. Send a request to the UDP port the runner is servering on). You can do that using
`cat walmart_test_query  > /dev/udp/127.0.0.1/53`


You should see a request for walmart.com on the backend.

If you want to use secure channels, you'll need to specify the cert.pem file to
either `runner.py` or `client.py`.

Note the backend you're connecting to must have a cooresponding cert / pk for
that common name specified.

For example the simple server / the cert and key in the directory uses localhost.
If the hostname doesn't work, gRPC will reject the query.

# TO GENERATE CERTS
Run:
`openssl req -newkey rsa:2048 -new -nodes -x509 -days 3650 -keyout key.pem -out cert.pem`

The servers pk will be key.pem and the certificate cert.pem.

You can use `openssl x509 -text -noout -in cert.pem` to inspect the cert.
