package com.kenzie.marketing.referral.service.caching;

import com.kenzie.marketing.referral.service.dependency.DaggerServiceComponent;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import java.util.Optional;

public class CacheClient {

    public String setValue(String key, int seconds, String value) {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        try (Jedis jedis = DaggerServiceComponent.create().provideJedis()) {
            jedis.setex(key, seconds, String.valueOf(value));
        }
        return key;
    }

    public Optional<String> getValue(String key) {
        try (Jedis jedis = DaggerServiceComponent.create().provideJedis()) {
            if (key == null) {
                throw new IllegalArgumentException();
            }
            return Optional.ofNullable(jedis.get(key));
        }
    }

    public boolean invalidate(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null.");
        }
        try (Jedis jedis = DaggerServiceComponent.create().provideJedis()) {
            Long keyExists = jedis.del(key);
            if (keyExists > 0) {
                return true;
            }
        }
        return false;
    }
}
