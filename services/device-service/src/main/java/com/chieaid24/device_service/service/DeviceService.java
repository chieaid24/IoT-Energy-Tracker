package com.chieaid24.device_service.service;

import com.chieaid24.device_service.client.UserClient;
import com.chieaid24.device_service.dto.DeviceDto;
import com.chieaid24.device_service.entity.Device;
import com.chieaid24.device_service.exception.DeviceNotFoundException;
import com.chieaid24.device_service.model.DeviceType;
import com.chieaid24.device_service.repository.DeviceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {
  private DeviceRepository deviceRepository;
  private UserClient userClient;

  public DeviceService(DeviceRepository deviceRepository, UserClient userClient) {
    this.deviceRepository = deviceRepository;
    this.userClient = userClient;
  }

  public DeviceDto getDeviceById(Long id) {
    Device device =
        deviceRepository
            .findById(id)
            .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));

    return mapToDto(device);
  }

  public DeviceDto createDevice(DeviceDto input) {
    Device createdDevice =
        Device.builder()
            .name(input.getName())
            .type(input.getType())
            .location(input.getLocation())
            .userId(input.getUserId())
            .build();
    Device saved = deviceRepository.save(createdDevice);
    return mapToDto(saved);
  }

  public DeviceDto updateDevice(Long id, DeviceDto input) {
    Device existingDevice =
        deviceRepository
            .findById(id)
            .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));

    existingDevice.setName(input.getName());
    existingDevice.setType(input.getType());
    existingDevice.setLocation(input.getLocation());
    existingDevice.setUserId(input.getUserId());

    Device updated = deviceRepository.save(existingDevice);
    return mapToDto(updated);
  }

  public void deleteDevice(Long id) {
    if (!deviceRepository.existsById(id)) {
      throw new DeviceNotFoundException("Device not found with id: " + id);
    }
    deviceRepository.deleteById(id);
  }

  public List<DeviceDto> getAllDevicesByUserId(Long userId) {
    List<Device> devices = deviceRepository.findAllByUserId(userId);
    return devices.stream().map(this::mapToDto).toList();
  }

  public void createDummyDevices(int devices) {

    // call to get the number of users from user-service
    int userCount = userClient.getUserCount();

    for (int i = 1; i <= devices; i++) {
      Device dummyDevice =
          Device.builder()
              .name("Dummy Device " + i)
              .type(DeviceType.values()[(i % DeviceType.values().length)])
              .location("Location " + ((i % 5) + 1))
              .userId((long) ((i % userCount) + 1)) // Assigning to user IDs 1 to 10
              .build();
      deviceRepository.save(dummyDevice);
    }
  }

  public Long getTotalDevices() {
    return deviceRepository.count();
  }

  private DeviceDto mapToDto(Device device) {
    return DeviceDto.builder()
        .id(device.getId())
        .name(device.getName())
        .type(device.getType())
        .location(device.getLocation())
        .userId(device.getUserId())
        .build();
  }
}
