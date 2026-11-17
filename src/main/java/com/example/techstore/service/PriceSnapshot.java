package com.example.techstore.service;

import java.math.BigDecimal;

/** Immutable values passed to workers; never contains JPA entities. */
public record PriceSnapshot(Integer article, String name, BigDecimal price) {}
