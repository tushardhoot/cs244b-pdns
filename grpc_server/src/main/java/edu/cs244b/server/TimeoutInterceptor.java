package edu.cs244b.server;

import io.grpc.*;

public class TimeoutInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler) {
        if (serverCall.isCancelled()) {
            throw Status.CANCELLED.withDescription("Cancelled by client").asRuntimeException();
        }

        return serverCallHandler.startCall(serverCall, metadata);
    }
}
