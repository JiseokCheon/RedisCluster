package com.example.demo;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.springframework.data.redis.connection.ClusterTopology;
import org.springframework.data.redis.connection.ClusterTopologyProvider;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.dynamic.RedisCommandFactory;

@RestController
public class RedisController {

	LettuceConnectionFactory redisClusterConnectionFactory;
	RedisCommandFactory redisCommandFactory;
	RedisTemplate<String, Object> redisTemplate;
	ValueOperations<String, Object> stringObjectValueOperations;
	StatefulRedisClusterConnection<String, String> connect;
	static int cnt = 0;

	@GetMapping("/get")
	public Object get(String key) {
		for (int i = 0; i < 100; i++) {
			try {
				// System.out.println(redisCommandFactory.getCommands(KeyCommands.class).get(key + "-" + i) + "-" + i);
				// System.out.println(this.connect.sync().get(key + "-" + i) + "-" + i);
				System.out.println(stringObjectValueOperations.get(key + "-" + i) + "-" + i);
				Thread.sleep(500);
			} catch (Exception e) {
				System.out.println("fail");
			}

		}
		// return redisCommandFactory.getCommands(KeyCommands.class).get(key);
		return stringObjectValueOperations.get(key);
		// return this.connect.sync().get(key);
	}

	// @PostMapping("/set")
	// public Object set(String key, String value) {
	// 	for (int i = 0; i < 100; i++) {
	// 		try {
	// 			// stringObjectValueOperations.set(key + "-" + i, value);
	// 			// System.out.println(i);
	// 			// System.out.println(
	// 			// 	redisCommandFactory.getCommands(KeyCommands.class).set(key + "-" + i, value) + " " + i);
	// 			System.out.println(this.connect.sync().set(key + "-" + i, value) + " " + i);
	// 			Thread.sleep(500);
	// 		} catch (Exception e) {
	// 			System.out.println("fail");
	// 		}
	// 	}
	// 	// return redisCommandFactory.getCommands(KeyCommands.class).get(key);
	// 	return this.connect.sync().get(key);
	// 	// return stringObjectValueOperations.get(key);
	// }

	@PostMapping("/set2")
	public Object set2(String key, String value) {
		Set<String> keys = new TreeSet<>();
		ScanOptions scanOptions = ScanOptions.scanOptions()
			.match(key + "*")
			.count(10)
			.build();
		Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection()
			.keyCommands().scan(scanOptions);
		while (cursor.hasNext()) {
			byte[] next = cursor.next();
			keys.add(new String(next, StandardCharsets.UTF_8));
		}
		System.out.println(keys);
		Set<String> keys1 = new TreeSet<>(Objects.requireNonNull(redisTemplate.keys(key + "*")));
		System.out.println(keys1);
		// KeyScanCursor<String> scan = connect.sync().scan(ScanArgs.Builder.matches(key + "*").limit(100));
		// System.out.println(scan.getKeys());
		//
		// while (!scan.isFinished()) {
		// 	System.out.println(scan.getKeys());
		// 	scan = connect.sync().scan(scan, ScanArgs.Builder.matches(key + "*").limit(10));
		// }

		for (int i = 0; i < 100; i++) {
			try {
				cnt++;
				stringObjectValueOperations.set(key + "-" + cnt, cnt);
				System.out.println(stringObjectValueOperations.get(key + "-" + cnt));
				Thread.sleep(500);
			} catch (Exception e) {
				System.out.println("fail");
			}
		}
		return stringObjectValueOperations.get(key);
	}

	public RedisController() {
		RedisClusterClient clusterClient = RedisClusterClient.create("redis://localhost:3000");

		ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
			// .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(10))
			.refreshTriggersReconnectAttempts(2)
			.enableAllAdaptiveRefreshTriggers()
			.build();

		clusterClient.setOptions(ClusterClientOptions.builder()
			// .maxRedirects(1)
			.autoReconnect(false)
			.timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(2)))
			.topologyRefreshOptions(clusterTopologyRefreshOptions)
			.build());

		this.connect = clusterClient.connect();
		this.redisCommandFactory = new RedisCommandFactory(this.connect);

		RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(
			Collections.singleton("localhost:3000"));

		ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
			.maxRedirects(5)
			.topologyRefreshOptions(clusterTopologyRefreshOptions)
			.build();

		LettuceClientConfiguration lettuceClientConfiguration = LettuceClientConfiguration.builder()
			.readFrom(ReadFrom.MASTER_PREFERRED)
			.clientOptions(clusterClientOptions)
			.build();

		this.redisClusterConnectionFactory = new LettuceConnectionFactory(redisClusterConfiguration,
			lettuceClientConfiguration);

		this.redisClusterConnectionFactory.afterPropertiesSet();

		this.redisTemplate = new RedisTemplate<>();
		this.redisTemplate.setConnectionFactory(this.redisClusterConnectionFactory);
		this.redisTemplate.setKeySerializer(new StringRedisSerializer());
		this.redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
		this.redisTemplate.afterPropertiesSet();
		//
		this.stringObjectValueOperations = this.redisTemplate.opsForValue();
	}
}
