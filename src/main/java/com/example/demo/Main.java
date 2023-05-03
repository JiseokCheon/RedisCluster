package com.example.demo;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.lettuce.core.KeyValue;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

@SpringBootApplication
public class Main {

	public static String workDir = "";
	public static String clusterConfigFileName = "cluster.cfg";
	public static List<String[]> serverList = new ArrayList<String[]>();

	public static RedisClusterClient clusterClient = null;
	public static StatefulRedisClusterConnection<String, String> connection = null;

	public static Util util = new Util();
	public static SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public static void main(String[] args) {
	}

	@SuppressWarnings({"unused", "resource"})
	public static void cluster() {
		clusterClient = RedisClusterClient.create("redis://localhost:3000");
		ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
			.enableAllAdaptiveRefreshTriggers()
			.build();

		clusterClient.setOptions(ClusterClientOptions.builder()
			.topologyRefreshOptions(topologyRefreshOptions)
			.build());

		connection = clusterClient.connect();
		connection.setReadFrom(ReadFrom.MASTER_PREFERRED);

		System.out.println("Connected to Redis");
		String timeFormat = "[" + format1.format(new Date()) + "] ";
		System.out.println("Start: " + timeFormat);

		int loop = 1000;
		Scanner scanner = new Scanner(System.in);
		String ret = null;

		while (true) {
			System.out.println("3:MSET       4:MGET");
			System.out.println("선택(exit:99)> ");
			String input = scanner.nextLine();
			int i = util.checkInt(input);
			switch (i) {
				case 3:
					mset(loop);
					break;
				case 4:
					mget(loop);
					break;
				case 99:
					break;
				default:
					System.out.println("잘못 선택했습니다. 다시 선택하세요. ");
			}

			if (i == 99)
				break;
		}

		connection.close();
		clusterClient.shutdown();
		System.out.println("Close");
	}

	// MSET
	public static void mset(int loop) {
		long time1 = System.currentTimeMillis();
		HashMap<String, String> map = new HashMap<>();
		for (int i = 1; i <= loop; i++) {
			String key = "keyMSET-" + String.format("%05d", i);
			String value = "valueMSET-" + String.format("%05d", i);
			if (i % 10 == 0) {
				try {
					util.sleep(100);    // failover test 시: 100ms 대기
					String timeFormat = "[" + format1.format(new Date()) + "] ";
					String ret = connection.sync().set(key, value);
					System.out.println(timeFormat + " --> " + ret);
				} catch (Exception e) {
					String timeFormat = "[" + format1.format(new Date()) + "] ";
					System.out.println(timeFormat + " --> Fail");
					i -= 10;
				}
				map.clear();
			}
		}
		long time2 = System.currentTimeMillis();
		String msg = util.elapse(time1, time2);
		System.out.println("Sync MSET: " + msg);
	}

	// MGET
	public static void mget(int loop) {
		long time1 = System.currentTimeMillis();
		String[] keys = new String[10];
		int j = 0;
		for (int i = 1; i <= loop; i++) {
			String key = "keyMSET-" + String.format("%05d", i);
			System.out.println(key);
			keys[j++] = key;
			if (i % 10 == 0) {
				try {
					util.sleep(100);    // failover test 시: 100ms 대기
					List<KeyValue<String, String>> ret = connection.sync().mget(keys);
					String timeFormat = "[" + format1.format(new Date()) + "] ";
					System.out.println(timeFormat + " --> " + ret.toString());
				} catch (Exception e) {
					String timeFormat = "[" + format1.format(new Date()) + "] ";
					System.out.println(timeFormat + " --> Fail");
					i -= 10;
				}
				j = 0;
			}
		}
		long time2 = System.currentTimeMillis();
		String msg = util.elapse(time1, time2);
		System.out.println("Sync MGET: " + msg);
	}
}