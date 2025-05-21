package com.example.walletapi.util;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public final class CastUtil {
	// Private constructor to prevent instantiation of this utility class
	private CastUtil() {
		throw new UnsupportedOperationException("The utility class " + this.getClass().getName()
				+ " cannot be instantiated");
	}

	public static <T> T cast(Object value, Class<T> type) throws ClassCastException, IllegalArgumentException {
		if (type == null) {
			throw new IllegalArgumentException("Cannot cast if you don't provide a type");
		}
		if (value == null) {
			throw new ClassCastException("Value is null, cannot cast to " + type.getName());
		}
		try {
			if (type == UUID.class) {
				return type.cast(UUID.fromString(value.toString()));
			} else if (type == Integer.class) {
				return type.cast(Integer.parseInt(value.toString()));
			} else if (type == Long.class) {
				return type.cast(Long.parseLong(value.toString()));
			} else if (type == Double.class) {
				return type.cast(Double.parseDouble(value.toString()));
			} else if (type == Boolean.class) {
				return type.cast(Boolean.parseBoolean(value.toString()));
			} else {
				return type.cast(value);
			}
		} catch (Exception e) {

			throw new ClassCastException(
					"Failed to cast " + value.getClass().getName() + " to " + type.getName() + ": " + e.getMessage());
		}
	}

	public static <T> T castSafe(Object value, Class<T> type) {
		try {
			return cast(value, type);
		} catch (ClassCastException e) {
			return null;
		}
	}

}
