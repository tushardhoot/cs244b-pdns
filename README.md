# pDNS

The pDNS implementation can be dividing into two main sections, the client and the server. 

* The client portion can be explored in `client/`, and contains a readme explaining the client.
* The server portion can be seen in `grpc_server/` and contains a readme explaining the server.
* The protobuf / gRPC service definition are under `schema/`.

## Certs
We've included an `ssl_cert` folder here, which we used as a simple way to sync certificates among our deployed servers.
While those specific certs likely won't be of interest, `ssl_certs/ssl_req.conf` will be of interest in generating your own
certificates using SAN (subject alternative name).

## Experimental data
Data for experiments ran can be found under both `perf` and `scripts`.

