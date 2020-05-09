So the runner.py will be the 'local server' that the client pings which will then
interact with a client for our P2PDNS Service, which will resolve that and then
our runner.py will resolve the browser / user's request.

# TO RUN
Launch your test server / backend (either ./test\_server.py or the javabackend)
Run the runner.py here
Send a request to the UDP port on localhost specifed that runner ran with (53 by default)

You should see a request for walmart.com on the backend.


TODO(kbaichoo): need to hoop up the runner with the client to send real DNS
requests, as well as handle real data.
