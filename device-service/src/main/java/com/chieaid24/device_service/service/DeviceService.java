package com.chieaid24.device_service.service;

import org.springframework.stereotype.Service;

import com.chieaid24.device_service.dto.DeviceDto;
import com.chieaid24.device_service.entity.Device;
import com.chieaid24.device_service.exception.DeviceNotFoundException;
import com.chieaid24.device_service.repository.DeviceRepository;

@Service
public class DeviceService {
    private DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public DeviceDto getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
            .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));
            
        return mapToDto(device);
    }

    public DeviceDto createDevice(DeviceDto input) {
        Device createdDevice = Device.builder()
            .name(input.getName())
            .type(input.getType())
            .location(input.getLocation())
            .userId(input.getUserId())
            .build();
        Device saved = deviceRepository.save(createdDevice);
        return mapToDto(saved);
    }

    public DeviceDto updateDevice(Long id, DeviceDto input) {
        Device existingDevice = deviceRepository.findById(id)
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
