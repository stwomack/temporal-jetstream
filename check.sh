echo Check Temporal is accessible
temporal operator namespace list

echo Check Homebrew services
brew services list
echo Should see: kafka and mongodb-community with status "started"

echo Check Flink Web UI
curl -s http://localhost:8081 > /dev/null && echo "Flink is running at localhost:8081" || echo "Flink is NOT running"

echo Check application is running
# curl http://localhost:8080/api/flights/AA1234/state?flightDate=2026-01-26 || echo "App ready for flights"
curl http://localhost:8080/api/flights/
