NUM_WORKERS=200

for WORKER_QPS in 5 10 20 40 80 160 320 640
do
  TOTAL_QPS=$(($NUM_WORKERS*$WORKER_QPS))
  echo "QPS of: $TOTAL_QPS"
  ./ghz --insecure --proto /home/ubuntu/cs244b/schema/src/main/proto/domain_lookup.proto  \
  --call  edu.cs244b.common.DomainLookupService.GetDomain -d '{"hostName": "google.com"}' \
    -c $NUM_WORKERS -q $WORKER_QPS -x 3m -n 1000000 127.0.0.1:9000 > "$TOTAL_QPS.txt"
done
