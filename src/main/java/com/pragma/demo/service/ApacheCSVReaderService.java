package com.pragma.demo.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
// import java.io.BufferedReader;
// import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service("ApacheCSVReader")
public class ApacheCSVReaderService implements CSVReaderService {
        public List<MarketDataEvent> readMarketDataEvents(String filePath) throws IOException {
                List<MarketDataEvent> events = new ArrayList<>();
                try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
                                Reader reader = new InputStreamReader(
                                                new BOMInputStream(inputStream),
                                                StandardCharsets.UTF_8);
                                CSVParser csvParser = new CSVParser(reader,
                                                CSVFormat.DEFAULT.withFirstRecordAsHeader()
                                                                .withTrim().withIgnoreHeaderCase()
                                                                .withIgnoreSurroundingSpaces())) {

                        // log.info("header names: {}", csvParser.getHeaderNames());
                        for (CSVRecord record : csvParser) {
                                // log.info("record: {}", record);
                                // log.info(record.get(0));
                                // log.info(record.get("Id"));
                                MarketDataEvent event = new MarketDataEvent(
                                                Integer.parseInt(record.get("Id")),
                                                Long.parseLong(record.get("Timestamp")),
                                                record.get("Event"),
                                                Double.parseDouble(record.get("Price1")),
                                                Integer.parseInt(record.get("Shares1")),
                                                record.get("Xchg1"),
                                                record.get("Price2").isBlank() ? 0.0
                                                                : Double.parseDouble(record
                                                                                .get("Price2")),
                                                record.get("Shares2").isBlank() ? 0
                                                                : Integer.parseInt(record
                                                                                .get("Shares2")),
                                                record.get("Xchg2"));
                                events.add(event);
                        }
                }
                return Collections.unmodifiableList(events);
        }
}
