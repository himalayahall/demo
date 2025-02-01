package com.pragma.demo.service;

import java.io.IOException;
import java.util.List;

public interface CSVReaderService {
    List<MarketDataEvent> readMarketDataEvents(String csvFilePath) throws IOException;
}
