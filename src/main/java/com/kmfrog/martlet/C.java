package com.kmfrog.martlet;

import java.time.LocalDate;
import java.time.ZoneId;

public interface C {
    
    long[] POWERS_OF_TEN = new long[] {
            1L,
            10L,
            100L,
            1000L,
            10000L,
            100000L,
            1000000L,
            10000000L,
            100000000L,
            1000000000L,
            10000000000L,
            100000000000L,
            1000000000000L,
            10000000000000L,
            100000000000000L,
            1000000000000000L,
            10000000000000000L,
            100000000000000000L,
            1000000000000000000L,
        };
    
    static final long EPOCH_MILLIS = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli();
    
    static final String TAC_API_BASE = "https://api.tac.vip";
    
    static final String ACCESS_KEY = "FD1fzz5kkhWQ4QeRSj13MPVEG1nHEBgE9B6zWd1Z6t6dC0wPrA30Qr0tEDGyldR0";
    
    static final String SECRET_KEY = "AJ3DZhn1AQ5d2byKkGXVSFKhFkGOxHrBQDHkhQQppk82CryVWqBFXuowAJpfvywa";


}
