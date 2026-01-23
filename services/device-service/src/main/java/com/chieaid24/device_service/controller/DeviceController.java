package com.chieaid24.device_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chieaid24.device_service.dto.DeviceDto;
import com.chieaid24.device_service.service.DeviceService;

/*
    TODO: Add getAllDevices endpoint with pagination and filtering by userId and deviceTypea
*/

@RestController
@RequestMapping("api/v1/device")
public class DeviceController {
    private DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceDto> getDeviceById(@PathVariable Long id) {
        DeviceDto deviceDto = deviceService.getDeviceById(id);
        if (deviceDto != null) {
            return ResponseEntity.ok(deviceDto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/total")
    public ResponseEntity<Long> getTotalDevices() {
        Long total = deviceService.getTotalDevices();
        return ResponseEntity.ok(total);
    }

    @PostMapping("/create")
    public ResponseEntity<DeviceDto> createDevice(@RequestBody DeviceDto deviceDto) {
        DeviceDto created = deviceService.createDevice(deviceDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/create/dummy")
    public ResponseEntity<Integer> createDummyDevices(@RequestParam int devices) {
        deviceService.createDummyDevices(devices);
        return ResponseEntity.status(HttpStatus.CREATED).body(devices);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceDto> updateDevice(@PathVariable Long id, @RequestBody DeviceDto deviceDto) {
        DeviceDto updated = deviceService.updateDevice(id, deviceDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DeviceDto>> getAllDevicesByUserId(
        @PathVariable Long userId
    ) {
        List<DeviceDto> devices = deviceService.getAllDevicesByUserId(userId);
        return ResponseEntity.ok(devices);
    }
    
}
