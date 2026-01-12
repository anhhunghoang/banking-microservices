package com.banking.common.util;

import java.util.UUID;

public class IdGenerator {

    public static UUID nextId() {
        return UUID.randomUUID();
    }
}
