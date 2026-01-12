package com.banking.account.event;

import com.banking.common.event.AccountCreated;

public interface AccountEventProducer {
    void sendAccountCreated(AccountCreated event);
}
