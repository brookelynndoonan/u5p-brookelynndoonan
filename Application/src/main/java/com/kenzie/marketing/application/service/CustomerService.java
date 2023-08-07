package com.kenzie.marketing.application.service;

import com.kenzie.marketing.application.controller.CustomerController;
import com.kenzie.marketing.application.controller.model.CreateCustomerRequest;
import com.kenzie.marketing.application.controller.model.CustomerResponse;
import com.kenzie.marketing.application.controller.model.LeaderboardUiEntry;
import com.kenzie.marketing.application.repositories.CustomerRepository;
import com.kenzie.marketing.application.repositories.model.CustomerRecord;
import com.kenzie.marketing.referral.model.*;
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

        return records.stream().map(this::customerResponseHelp).collect(Collectors.toList());
    }

    /**
     * findByCustomerId
     * @param customerId
     * @return The Customer with the given customerId
     */
    public CustomerResponse getCustomer(String customerId) {
            Optional<CustomerRecord> record = customerRepository.findById(customerId);
            return record.map(this::customerResponseHelp)
                    .orElse(null);

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

        return customerResponseHelp(customerRecord);
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

        return  referralServiceClient.getDirectReferrals(customerId)
                .stream()
                .map(Referral::getCustomerId)
                .map(this::getCustomer)
                .collect(Collectors.toList());

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

        if(customerRecord.getReferrerId() == null){
           customerResponse.setReferrerName("");
        } else{

            customerResponse.setReferrerName(customerRepository.findById(customerRecord.getReferrerId())
                    .map(CustomerRecord::getName)
                    .orElse(""));

        }
        return customerResponse;
    }


}
