echo Check Temporal is accessible
temporal operator namespace list

echo Check Docker services
docker-compose ps
echo Should see: kafka, zookeeper, mongodb all running

echo Check application is running
# curl http://localhost:8080/api/flights/AA1234/state?flightDate=2026-01-26 || echo "App ready for flights"
curl http://localhost:8080/api/flights/
