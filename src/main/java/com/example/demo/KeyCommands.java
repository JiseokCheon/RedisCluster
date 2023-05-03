package com.example.demo;

import io.lettuce.core.dynamic.Commands;

public interface KeyCommands extends Commands {

	String get(String key);

	String set(String key, String value);

	String set(String key, byte[] value);
}