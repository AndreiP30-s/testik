package com.example.techstore.service;

import java.math.BigDecimal;

/** Immutable values passed to workers; never contains JPA entities. */
public record PriceSnapshot(long article, String name, BigDecimal price) {}
