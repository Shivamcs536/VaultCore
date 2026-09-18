package com.vaultcore.model;

/**
 * A premium customer gets a 100% discount on service fees.
 * Overrides the base Customer's behaviour - inheritance + polymorphism.
 */
public class PremiumCustomer extends Customer {

    public PremiumCustomer(String customerId, String name, String email) {
        super(customerId, name, email);
    }

    @Override
    public double getServiceFeeDiscountPercent() {
        return 100.0;
    }

    @Override
    public String toString() {
        return super.toString() + " [PREMIUM]";
    }
}
