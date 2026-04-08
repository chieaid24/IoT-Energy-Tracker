let CONFIG = {
  endpoint: "http://<your-ip>:8082/api/v1/ingestion/shelly/1", // Update to your server location
  interval_sec: 5  // how often to POST, in seconds
};

function postEnergyData() {
  let status = Shelly.getComponentStatus("switch", 0);

  if (status === null) {
    print("Failed to get switch status");
    return;
  }

  let body = JSON.stringify({
    apower: status.apower,
    voltage: status.voltage,
    current: status.current,
    aenergy: status.aenergy,
    temperature: status.temperature
  });

  Shelly.call("HTTP.POST", {
    url: CONFIG.endpoint,
    body: body,
    content_type: "application/json"
  }, function (result, error_code, error_message) {
    if (error_code !== 0) {
      print("HTTP.POST failed:", error_message);
    } else {
      print("Posted energy data successfully");
    }
  });
}

Timer.set(CONFIG.interval_sec * 1000, true, postEnergyData);

print("Energy reporting script started");