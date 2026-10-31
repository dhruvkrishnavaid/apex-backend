package com.apex.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Human readable document numbers such as {@code PO-20261031-K3F9QZ}. */
public final class ReferenceNumbers {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

  private ReferenceNumbers() {
  }

  public static String next(String prefix) {
    StringBuilder suffix = new StringBuilder();
    for (int i = 0; i < 6; i++) {
      suffix.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return prefix + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
  }
}
