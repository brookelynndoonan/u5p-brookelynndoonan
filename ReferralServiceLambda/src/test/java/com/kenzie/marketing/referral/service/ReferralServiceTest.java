package com.kenzie.marketing.referral.service;


import com.kenzie.marketing.referral.model.*;
import com.kenzie.marketing.referral.service.converter.ZonedDateTimeConverter;
import com.kenzie.marketing.referral.service.dao.ReferralDao;
import com.kenzie.marketing.referral.service.exceptions.InvalidDataException;
import com.kenzie.marketing.referral.service.model.ReferralRecord;
import net.andreinc.mockneat.MockNeat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.sql.Ref;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReferralServiceTest {

    /**
     * ------------------------------------------------------------------------
     * expenseService.getExpenseById
     * ------------------------------------------------------------------------
     **/

    private ReferralDao referralDao;
    private ReferralService referralService;

    @BeforeAll
    void setup() {
        this.referralDao = mock(ReferralDao.class);
        this.referralService = new ReferralService(referralDao);
    }

    @Test
    void addReferralTest() {
        ArgumentCaptor<ReferralRecord> referralCaptor = ArgumentCaptor.forClass(ReferralRecord.class);

        // GIVEN
        String customerId = "fakecustomerid";
        String referrerId = "fakereferralid";
        ReferralRequest request = new ReferralRequest();
        request.setCustomerId(customerId);
        request.setReferrerId(referrerId);

        // WHEN
        ReferralResponse response = this.referralService.addReferral(request);

        // THEN
        verify(referralDao, times(1)).addReferral(referralCaptor.capture());
        ReferralRecord record = referralCaptor.getValue();

        assertNotNull(record, "The record is valid");
        assertEquals(customerId, record.getCustomerId(), "The record customerId should match");
        assertEquals(referrerId, record.getReferrerId(), "The record referrerId should match");
        assertNotNull(record.getDateReferred(), "The record referral date exists");

        assertNotNull(response, "A response is returned");
        assertEquals(customerId, response.getCustomerId(), "The response customerId should match");
        assertEquals(referrerId, response.getReferrerId(), "The response referrerId should match");
        assertNotNull(response.getReferralDate(), "The response referral date exists");
    }

    @Test
    void addReferralTest_no_customer_id() {
        // GIVEN
        String customerId = "";
        String referrerId = "";
        ReferralRequest request = new ReferralRequest();
        request.setCustomerId(customerId);
        request.setReferrerId(referrerId);

        // WHEN / THEN
        assertThrows(InvalidDataException.class, () -> this.referralService.addReferral(request));
    }

    @Test
    void getDirectReferralsTest() {
        // GIVEN
        String customerId = "fakecustomerid";
        List<ReferralRecord> recordList = new ArrayList<>();

        ReferralRecord record1 = new ReferralRecord();
        record1.setCustomerId("customer1");
        record1.setReferrerId(customerId);
        record1.setDateReferred(ZonedDateTime.now());
        recordList.add(record1);

        ReferralRecord record2 = new ReferralRecord();
        record2.setCustomerId("customer2");
        record2.setReferrerId(customerId);
        record2.setDateReferred(ZonedDateTime.now());
        recordList.add(record2);

        when(referralDao.findByReferrerId(customerId)).thenReturn(recordList);

        // WHEN
        List<Referral> referrals = this.referralService.getDirectReferrals(customerId);

        // THEN
        verify(referralDao, times(1)).findByReferrerId(customerId);

        assertNotNull(referrals, "The returned referral list is valid");
        assertEquals(2, referrals.size(), "The referral list has 2 items");
        for (Referral referral : referrals) {
            if (record1.getCustomerId().equals(referral.getCustomerId())) {
                assertEquals(record1.getReferrerId(), customerId);
                assertEquals(new ZonedDateTimeConverter().convert(record1.getDateReferred()), referral.getReferralDate());
            } else if (record2.getCustomerId().equals(referral.getCustomerId())) {
                assertEquals(record2.getReferrerId(), customerId);
                assertEquals(new ZonedDateTimeConverter().convert(record2.getDateReferred()), referral.getReferralDate());
            } else {
                fail("A Referral was returned that does not match record 1 or 2.");
            }
        }
    }

    @Test
    void getCustomerReferralSummary_returnReferrals_isSuccessful() {
        int amountReferral1 = 0;
        int amountReferral2 = 0;
        int amountReferral3 = 0;
        String customerId = "customerId";
        String referrerId = "referrerId";
        CustomerReferrals referral = new CustomerReferrals();

        Referral directReferrals = new Referral();
        List<Referral> referrals1 = referralService.getDirectReferrals(customerId);
        referrals1.add(directReferrals);

        referralDao.findByReferrerId(referrerId);
        referral.setNumFirstLevelReferrals(amountReferral1);
        referral.setNumSecondLevelReferrals(amountReferral2);
        referral.setNumThirdLevelReferrals(amountReferral3);

        Assertions.assertNotNull(referralService.getCustomerReferralSummary(customerId));

    }

    @Test
    void getCustomerReferralSummary_customerIdNull_throwException() {
        String customerId = "";
        assertThrows(InvalidDataException.class,
                () -> referralService.getCustomerReferralSummary(customerId));
    }

    @Test
    void deleteReferrals_listCustomerIdsIsNull() {
        List<String> customerIds = null;
        assertThrows(InvalidDataException.class,
                () -> referralService.deleteReferrals(customerIds));
    }

    @Test
    void deleteReferrals_deletesRecord() {
        ReferralRecord referralRecord = new ReferralRecord();
        String customerId = "customerId";
        List<String> customerIds = new ArrayList<>();
        customerIds.add(customerId);

        when(referralDao.deleteReferral(referralRecord)).thenReturn(true);

        Assertions.assertFalse(referralService.deleteReferrals(customerIds));

    }

    @Test
    void deleteReferrals_customerIdIsNull() {
        List<String> customerIds = new ArrayList<>();
        customerIds.add(null);

        assertThrows(InvalidDataException.class,
                () -> referralService.deleteReferrals(customerIds));
    }


}