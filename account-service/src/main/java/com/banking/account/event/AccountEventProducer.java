package com.banking.account.event;

import com.banking.common.event.AccountCreated;
import java.util.UUID;
import com.banking.common.event.MoneyCredited;
import com.banking.common.event.MoneyDebited;
import com.banking.common.event.MoneyReserved;
import com.banking.common.event.ReservationFailed;

public interface AccountEventProducer {
    void sendAccountCreated(AccountCreated event);

    void sendMoneyCredited(MoneyCredited event, UUID transactionId);

    void sendMoneyDebited(MoneyDebited event, UUID transactionId);

    void sendMoneyReserved(MoneyReserved event, UUID transactionId);

    void sendReservationFailed(ReservationFailed event, UUID transactionId);

    void sendRefundCompleted(com.banking.common.event.RefundCompleted event, UUID transactionId);
}
