package com.pragma.demo.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service("JacksonCSVReader")
public class JacksonCSVReaderService implements CSVReaderService {
        public List<MarketDataEvent> readMarketDataEvents(String csvFilePath) throws IOException {
                try (InputStream inputStream = Files.newInputStream(Paths.get(csvFilePath));) {
                        CsvMapper csvMapper = new CsvMapper();
                        csvMapper.setPropertyNamingStrategy(
                                        PropertyNamingStrategies.UPPER_CAMEL_CASE);

                        CsvSchema schema = CsvSchema.emptySchema().withHeader(); // Use first row as
                                                                                 // header
                        MappingIterator<MarketDataEvent> mappingIterator =
                                        csvMapper.readerFor(MarketDataEvent.class).with(schema)
                                                        .readValues(inputStream);

                        return Collections.unmodifiableList(mappingIterator.readAll());
                }
        }
}

