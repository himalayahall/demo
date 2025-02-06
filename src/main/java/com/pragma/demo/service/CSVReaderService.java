package com.pragma.demo.service;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.Resource;
import com.pragma.demo.models.MarketDataEvent;

public interface CSVReaderService {
    List<MarketDataEvent> readMarketDataEvents(Resource resource) throws IOException;
}
