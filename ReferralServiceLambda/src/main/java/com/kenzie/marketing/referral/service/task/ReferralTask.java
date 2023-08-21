package com.kenzie.marketing.referral.service.task;

import com.kenzie.marketing.referral.model.LeaderboardEntry;
import com.kenzie.marketing.referral.service.ReferralService;
import com.kenzie.marketing.referral.service.dao.ReferralDao;
import com.kenzie.marketing.referral.service.model.ReferralRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ReferralTask implements Callable {
    private ReferralDao referralDao;
    private ReferralRecord referralNode;
    private ReferralService referralService;

    public ReferralTask(ReferralDao referralDao, ReferralRecord referralRecord, ReferralService referralService) {
        this.referralDao = referralDao;
        this.referralNode = referralRecord;
        this.referralService = referralService;
    }

    @Override
    public List<LeaderboardEntry> call() {

        List<ReferralRecord> customerIDs = referralDao.findUsersWithoutReferrerId();
        List<LeaderboardEntry> topReferrals = new ArrayList<>();

        for (ReferralRecord customerId : customerIDs) {
            int numReferrals = referralService.getDirectReferrals(customerId.getCustomerId()).size();

            LeaderboardEntry newEntry = new LeaderboardEntry(numReferrals, customerId.getCustomerId());

            if (topReferrals.isEmpty()) {
                topReferrals.add(newEntry);
            } else {
                int leaderboardReferrals = referralCountHelperMethod(topReferrals, numReferrals);
                if (leaderboardReferrals >= 0 && leaderboardReferrals < 5) {
                    topReferrals.add(leaderboardReferrals, newEntry);
                    if (topReferrals.size() > 5) {
                        topReferrals.remove(5);
                    }
                }
            }
        }
        return topReferrals;
    }

    private int referralCountHelperMethod(List<LeaderboardEntry> leaderboard, int newReferrals) {
        for (int i = 0; i < leaderboard.size(); i++) {
            if (newReferrals > leaderboard.get(i).getNumReferrals()) {
                return i;
            }
        }
        return leaderboard.size();
    }
}
