package com.research.experimentplatform.dto;

import jakarta.validation.constraints.AssertTrue;

public record AcceptInvitationRequest(
    @AssertTrue(message = "You must agree to the consent form to participate")
    boolean consentAgreed
) {}
