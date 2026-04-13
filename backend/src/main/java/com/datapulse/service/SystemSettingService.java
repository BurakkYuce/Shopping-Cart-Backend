package com.datapulse.service;

import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.model.SystemSetting;
import com.datapulse.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository repository;

    public List<SystemSetting> getAll() {
        return repository.findAll();
    }

    public SystemSetting get(String key) {
        return repository.findById(key)
                .orElseThrow(() -> new EntityNotFoundException("SystemSetting", key));
    }

    public SystemSetting update(String key, String value) {
        SystemSetting setting = get(key);
        setting.setSettingValue(value);
        return repository.save(setting);
    }
}
