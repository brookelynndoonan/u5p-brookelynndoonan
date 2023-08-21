package com.kenzie.marketing.referral.service.caching;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.kenzie.marketing.referral.service.dao.NonCachingReferralDao;
import com.kenzie.marketing.referral.service.dao.ReferralDao;
import com.kenzie.marketing.referral.service.model.ReferralRecord;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

// Got some help from Michael about where to put the Gson and Gson methods
// and the findByReferrerId method
public class CachingReferralDao implements ReferralDao {
    private static final int REFERRAL_READ_TTL = 60 * 60;

    private final CacheClient cacheClient;

    private final NonCachingReferralDao nonCachingReferralDao;

    private static String referenceKey = "ReferenceKey::";

    public CachingReferralDao(CacheClient cacheClient, NonCachingReferralDao nonCachingReferralDao) {
        this.cacheClient = cacheClient;
        this.nonCachingReferralDao = nonCachingReferralDao;
    }

    GsonBuilder builder = new GsonBuilder().registerTypeAdapter(
            ZonedDateTime.class,
            new TypeAdapter<ZonedDateTime>() {
                @Override
                public void write(JsonWriter out, ZonedDateTime value) throws IOException {
                    out.value(value.toString());
                }

                @Override
                public ZonedDateTime read(JsonReader in) throws IOException {
                    return ZonedDateTime.parse(in.nextString());
                }
            }
    ).enableComplexMapKeySerialization();

    Gson gson = builder.create();

    private List<ReferralRecord> fromJson(String json) {
        return gson.fromJson(json, new TypeToken<ArrayList<ReferralRecord>>() {
        }.getType());
    }

    // Setting value
    private void addToCache(List<ReferralRecord> records) {
        cacheClient.setValue(referenceKey,
                REFERRAL_READ_TTL,
                gson.toJson(records)
        );
    }

    /**
     * Allows access to and manipulation of Match objects from the data store.
     */

    public ReferralRecord addReferral(ReferralRecord referral) {
        referenceKey += referral.getCustomerId();
        cacheClient.invalidate(referenceKey);
        return nonCachingReferralDao.addReferral(referral);
    }

    public boolean deleteReferral(ReferralRecord referral) {
        referenceKey += referral.getReferrerId();
        cacheClient.invalidate(referenceKey);
        return nonCachingReferralDao.deleteReferral(referral);
    }

    public List<ReferralRecord> findByReferrerId(String referrerId) {
        referenceKey += referrerId;

        return cacheClient.getValue(referrerId)
                .map(this::fromJson)
                .orElseGet(() -> {
                    List<ReferralRecord> referralRecords = nonCachingReferralDao.findByReferrerId(referrerId);
                    addToCache(referralRecords);
                    return referralRecords;
                });
    }

    public List<ReferralRecord> findUsersWithoutReferrerId() {

        return nonCachingReferralDao.findUsersWithoutReferrerId();
    }

}