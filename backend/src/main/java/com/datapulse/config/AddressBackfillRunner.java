package com.datapulse.config;

import com.datapulse.model.Address;
import com.datapulse.model.User;
import com.datapulse.repository.AddressRepository;
import com.datapulse.repository.UserRepository;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Backfills a default address for every user that has none, using cities from
 * customer_behavior.csv so admin analytics has meaningful geo distribution.
 * Runs after DataLoader (CommandLineRunner order default is 0; we pick 10).
 * Idempotent: skips users that already own at least one address.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class AddressBackfillRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Value("${app.addresses.behavior-csv:/datasets/customer_behavior.csv}")
    private String behaviorCsvPath;

    @Override
    public void run(String... args) {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.info("AddressBackfill: no users — nothing to do.");
            return;
        }

        List<String> usersWithoutAddress = new ArrayList<>();
        for (User u : users) {
            if (addressRepository.findByUserId(u.getId()).isEmpty()) {
                usersWithoutAddress.add(u.getId());
            }
        }
        if (usersWithoutAddress.isEmpty()) {
            log.info("AddressBackfill: every user already has an address — skipping.");
            return;
        }

        List<String> cityPool;
        try {
            cityPool = readCities(behaviorCsvPath);
        } catch (Exception e) {
            log.warn("AddressBackfill: could not read {}: {}", behaviorCsvPath, e.getMessage());
            return;
        }
        if (cityPool.isEmpty()) {
            log.warn("AddressBackfill: city pool empty from {}", behaviorCsvPath);
            return;
        }

        log.info("AddressBackfill: seeding default address for {} user(s) using {} cities.",
                usersWithoutAddress.size(), cityPool.size());

        List<Address> batch = new ArrayList<>();
        for (int i = 0; i < usersWithoutAddress.size(); i++) {
            String userId = usersWithoutAddress.get(i);
            String city = cityPool.get(i % cityPool.size());

            Address a = new Address();
            a.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            a.setUserId(userId);
            a.setTitle("Home");
            a.setFullName("DataPulse Customer");
            a.setAddressLine1("—");
            a.setCity(city);
            a.setCountry("United States");
            a.setIsDefault(true);
            batch.add(a);

            if (batch.size() >= 500) {
                addressRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            addressRepository.saveAll(batch);
        }
        log.info("AddressBackfill: done — {} addresses seeded.", usersWithoutAddress.size());
    }

    private List<String> readCities(String path) throws Exception {
        try (FileReader reader = new FileReader(path)) {
            List<BehaviorRow> rows = new CsvToBeanBuilder<BehaviorRow>(reader)
                    .withType(BehaviorRow.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
            List<String> cities = new ArrayList<>();
            for (BehaviorRow r : rows) {
                if (r.city != null && !r.city.isBlank()) cities.add(r.city.trim());
            }
            return cities;
        }
    }

    public static class BehaviorRow {
        @CsvBindByName(column = "City") public String city;
    }
}
