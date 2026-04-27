package com.demo.ebook;

import java.time.Instant;

public record PolicyFingerprint(
        String rootPolicyId,
        String rootPolicyVersion,
        Instant updatedAt
) {
}

/*
Tự reload
*/
