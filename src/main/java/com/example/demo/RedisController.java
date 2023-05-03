package com.example.demo;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.redis.connection.RedisClusterConfiguration;
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

import io.lettuce.core.ReadFrom;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;

@RestController
public class RedisController {

	LettuceConnectionFactory redisClusterConnectionFactory;
	RedisTemplate<String, Object> redisTemplate;
	ValueOperations<String, Object> stringObjectValueOperations;
	static int cnt = 0;

	@GetMapping("/get")
	public Object get(String key) {
		for (int i = 0; i < 100; i++) {
			try {
				System.out.println(stringObjectValueOperations.get(key + "-" + i) + "-" + i);
				Thread.sleep(500);
			} catch (Exception e) {
				System.out.println("fail");
			}

		}
		return stringObjectValueOperations.get(key);
	}

	@PostMapping("/set")
	public Object set(String key, String value) {
		Set<String> keys = new TreeSet<>();
		ScanOptions scanOptions = ScanOptions.scanOptions()
			.match(key + "*")
			.count(10)
			.build();

		// Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection()
		// 	.keyCommands().scan(scanOptions);
		try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
			.getConnection()
			.keyCommands()
			.scan(scanOptions)) {
			while (cursor.hasNext()) {
				byte[] next = cursor.next();
				keys.add(new String(next, StandardCharsets.UTF_8));
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println(e.getCause().getLocalizedMessage());
		}

		System.out.println(keys);

		Set<String> keys1 = new TreeSet<>(Objects.requireNonNull(redisTemplate.keys(key + "*")));
		System.out.println(keys1);

		for (int i = 0; i < 100; i++) {
			try {
				cnt++;
				stringObjectValueOperations.set(key + "-" + cnt, cnt);
				redisTemplate.unlink(key + "-" + cnt);
				System.out.println(key + "-" + cnt + "   " +stringObjectValueOperations.get(key + "-" + cnt));
				Thread.sleep(500);
			} catch (Exception e) {
				System.out.println("fail");
			}
		}
		return stringObjectValueOperations.get(key);
	}

	public RedisController() {
		ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
			.refreshTriggersReconnectAttempts(2)
			.enableAllAdaptiveRefreshTriggers()
			.build();

		RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(
			Collections.singleton("localhost:3000"));

		ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
			.topologyRefreshOptions(clusterTopologyRefreshOptions)
			.build();

		LettuceClientConfiguration lettuceClientConfiguration = LettuceClientConfiguration.builder()
			.readFrom(ReadFrom.ANY)
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
