package com.pragma.demo.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.pragma.demo.models.MarketDataEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service("JacksonCSVReader")
public class JacksonCSVReaderService implements CSVReaderService {
        public List<MarketDataEvent> readMarketDataEvents(Resource resource) throws IOException {
                try (InputStream inputStream = resource.getInputStream();) {
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

