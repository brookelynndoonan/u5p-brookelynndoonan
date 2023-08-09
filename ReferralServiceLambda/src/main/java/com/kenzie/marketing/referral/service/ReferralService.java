package com.kenzie.marketing.referral.service;

import com.kenzie.marketing.referral.model.CustomerReferrals;
import com.kenzie.marketing.referral.model.LeaderboardEntry;
import com.kenzie.marketing.referral.model.Referral;
import com.kenzie.marketing.referral.model.ReferralRequest;
import com.kenzie.marketing.referral.model.ReferralResponse;
import com.kenzie.marketing.referral.service.converter.ReferralConverter;
import com.kenzie.marketing.referral.service.dao.ReferralDao;
import com.kenzie.marketing.referral.service.exceptions.InvalidDataException;
import com.kenzie.marketing.referral.service.model.ReferralRecord;

import javax.inject.Inject;

import java.sql.Ref;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.stream.Collectors;

public class ReferralService {

    private ReferralDao referralDao;
    private ExecutorService executor;

    @Inject
    public ReferralService(ReferralDao referralDao) {
        this.referralDao = referralDao;
        this.executor = Executors.newCachedThreadPool();
    }

    // Necessary for testing, do not delete
    public ReferralService(ReferralDao referralDao, ExecutorService executor) {
        this.referralDao = referralDao;
        this.executor = executor;
    }

    public List<LeaderboardEntry> getReferralLeaderboard() {
        // Task 3 Code Here
        return null;
    }

    // Got help from Munir about nested loop concept.
    // Elise helped me end the method more around the concept of collecting the referrals for second
    // and third level,rather than trying to add them all together.
    public CustomerReferrals getCustomerReferralSummary(String customerId) {
        if (customerId == null || customerId.isEmpty()) {
            throw new InvalidDataException("Customer should not have null or empty Id.");
        }
        int amountFirstReferral = 0;
        int amountSecondReferral = 0;
        int amountThirdReferral = 0;

        CustomerReferrals referrals = new CustomerReferrals();

        List<Referral> firstLevel = getDirectReferrals(customerId);
          referrals.setNumFirstLevelReferrals(firstLevel.size());

        for (Referral firstReferralBatch : firstLevel) {
            List<Referral> secondLevel = getDirectReferrals(firstReferralBatch.getCustomerId());
            amountSecondReferral += secondLevel.size();

            for (Referral secondReferralBatch : secondLevel) {
                List<Referral> thirdLevel = getDirectReferrals(secondReferralBatch.getCustomerId());
                amountThirdReferral += thirdLevel.size();
            }
        }

        referrals.setNumSecondLevelReferrals(amountSecondReferral);
        referrals.setNumThirdLevelReferrals(amountThirdReferral);

        return referrals;
    }


    public List<Referral> getDirectReferrals(String customerId) {

        List<ReferralRecord> records = referralDao.findByReferrerId(customerId);
        return records.stream()
                .map(ReferralConverter::fromRecordToReferral)
                .collect(Collectors.toList());
    }


    public ReferralResponse addReferral(ReferralRequest referral) {
        if (referral == null || referral.getCustomerId() == null || referral.getCustomerId().length() == 0) {
            throw new InvalidDataException("Request must contain a valid Customer ID");
        }
        ReferralRecord record = ReferralConverter.fromRequestToRecord(referral);
        referralDao.addReferral(record);
        return ReferralConverter.fromRecordToResponse(record);
    }

    public boolean deleteReferrals(List<String> customerIds){
        boolean allDeleted = true;

        if(customerIds == null){
            throw new InvalidDataException("Request must contain a valid list of Customer ID");
        }

        for(String customerId : customerIds){
            if(customerId == null || customerId.length() == 0){
                throw new InvalidDataException("Customer ID cannot be null or empty to delete");
            }

            ReferralRecord record = new ReferralRecord();
            record.setCustomerId(customerId);

            boolean deleted = referralDao.deleteReferral(record);

            if(!deleted){
                allDeleted = false;
            }
        }
        return allDeleted;
    }
}
