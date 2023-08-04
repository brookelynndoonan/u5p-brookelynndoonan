package com.kenzie.marketing.application.service;

import com.kenzie.marketing.application.controller.CustomerController;
import com.kenzie.marketing.application.controller.model.CreateCustomerRequest;
import com.kenzie.marketing.application.controller.model.CustomerResponse;
import com.kenzie.marketing.application.controller.model.LeaderboardUiEntry;
import com.kenzie.marketing.application.repositories.CustomerRepository;
import com.kenzie.marketing.application.repositories.model.CustomerRecord;
import com.kenzie.marketing.referral.model.CustomerReferrals;
import com.kenzie.marketing.referral.model.LeaderboardEntry;
import com.kenzie.marketing.referral.model.Referral;
import com.kenzie.marketing.referral.model.ReferralRequest;
import com.kenzie.marketing.referral.model.client.ReferralServiceClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.UUID.randomUUID;

@Service
public class CustomerService {
    private static final Double REFERRAL_BONUS_FIRST_LEVEL = 10.0;
    private static final Double REFERRAL_BONUS_SECOND_LEVEL = 3.0;
    private static final Double REFERRAL_BONUS_THIRD_LEVEL = 1.0;

    private CustomerRepository customerRepository;
    private ReferralServiceClient referralServiceClient;

    public CustomerService(CustomerRepository customerRepository, ReferralServiceClient referralServiceClient) {
        this.customerRepository = customerRepository;
        this.referralServiceClient = referralServiceClient;
    }

    /**
     * findAllCustomers
     * @return A list of Customers
     */
    public List<CustomerResponse> findAllCustomers() {
        List<CustomerRecord> records = StreamSupport.stream(customerRepository.findAll().spliterator(), true).collect(Collectors.toList());

        // Task 1 - Add your code here

        return null;
    }

    /**
     * findByCustomerId
     * @param customerId
     * @return The Customer with the given customerId
     */
    public CustomerResponse getCustomer(String customerId) {
        Optional<CustomerRecord> record = customerRepository.findById(customerId);

        // Task 1 - Add your code here

        return null;
    }

    /**
     * addNewCustomer
     *
     * This creates a new customer.  If the referrerId is included, the referrerId must be valid and have a
     * corresponding customer in the DB.  This posts the referrals to the referral service
     * @param createCustomerRequest
     * @return A CustomerResponse describing the customer
     */
    public CustomerResponse addNewCustomer(CreateCustomerRequest createCustomerRequest) {
        // 1: There are two options for the referrerId - it's either empty (meaning the customer joined by themselves)
        // or contains a value (meaning another customer with that ID referred them).
        // If the request contains a referrerId, then that must be a valid customer id from the table.
        // If that referrerId does not exist in the Customer table, the request should be rejected.
        // 2: The CustomerRecord should be created and saved into the Customer table. To create the customer ID,
        // use a call to record.setId(randomUUID().toString()).
        // 3: A call should be made to referralServiceClient.addReferral() to add the new customer.
        // It is important that the referralServiceClient.addReferral() method is called for every customer
        // added, even if they were not referred! If a customer had no referrer, you should still call
        // addReferral() using a blank referrerId.
        // 4:A response object should be created and returned with all the necessary information.
        CustomerRecord customerRecord = new CustomerRecord();
        customerRecord.setId(randomUUID().toString());
        customerRecord.setDateCreated(LocalDateTime.now().toString()); //possible ZoneDateTime.now().toString()
        customerRecord.setName(createCustomerRequest.getName());

        ReferralRequest referralRequest;

        if(createCustomerRequest.getReferrerId().isEmpty()) {

        referralRequest = new ReferralRequest(customerRecord.getId(), "");

        }
        else {
            if (!customerRepository.existsById(createCustomerRequest.getReferrerId()
                    .get())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer not found.");
            } else {
                customerRecord.setReferrerId(createCustomerRequest.getReferrerId().get());

                referralRequest = new ReferralRequest(customerRecord.getId(),  createCustomerRequest
                        .getReferrerId().get());

            }
        }
        customerRepository.save(customerRecord);
        referralServiceClient.addReferral(referralRequest);

        return customerResponseHelp(customerRecord);
    }

    /**
     * updateCustomer - This updates the customer name for the given customer id
     * @param customerId - The Id of the customer to update
     * @param customerName - The new name for the customer
     */
    public CustomerResponse updateCustomer(String customerId, String customerName) {
        Optional<CustomerRecord> customerExists = customerRepository.findById(customerId);
        if (customerExists.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer Not Found");
        }
        CustomerRecord customerRecord = customerExists.get();
        customerRecord.setName(customerName);
        customerRepository.save(customerRecord);

        // Task 1 - Add your code here

        return null;
    }

    /**
     * deleteCustomer - This deletes the customer record for the given customer id
     * @param customerId
     */
    public void deleteCustomer(String customerId) {
        customerRepository.deleteById(customerId);
    }

    /**
     * calculateBonus - This calculates the referral bonus for the given customer according to the referral bonus
     * constants.
     * @param customerId
     * @return
     */
    public Double calculateBonus(String customerId) {
        CustomerReferrals referrals = referralServiceClient.getReferralSummary(customerId);

        Double calculationResult = REFERRAL_BONUS_FIRST_LEVEL * referrals.getNumFirstLevelReferrals() +
                REFERRAL_BONUS_SECOND_LEVEL * referrals.getNumSecondLevelReferrals() +
                REFERRAL_BONUS_THIRD_LEVEL * referrals.getNumThirdLevelReferrals();

        return calculationResult;
    }

    /**
     * getReferrals - This returns a list of referral entries for every customer directly referred by the given
     * customerId.
     * @param customerId
     * @return
     */
    public List<CustomerResponse> getReferrals(String customerId) {

        // Task 1 - Add your code here

        return null;
    }

    /**
     * getLeaderboard - This calls the referral service to retrieve the current top 5 leaderboard of the most referrals
     * @return
     */
    public List<LeaderboardUiEntry> getLeaderboard() {

        // Task 2 - Add your code here

        return null;
    }

    /* -----------------------------------------------------------------------------------------------------------
        Private Methods
       ----------------------------------------------------------------------------------------------------------- */

    private CustomerResponse customerResponseHelp (CustomerRecord customerRecord) {
        CustomerResponse customerResponse = new CustomerResponse();
        customerResponse.setName(customerRecord.getName());
        customerResponse.setId(customerRecord.getId());
        customerResponse.setDateJoined(customerRecord.getDateCreated());
        customerResponse.setReferrerId(customerRecord.getReferrerId());
        customerResponse.setReferrerName(customerRepository.findById(customerRecord.getReferrerId())
                .map(CustomerRecord::getName)
                .orElse(""));
        return customerResponse;
    }


}
