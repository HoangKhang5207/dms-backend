package com.genifast.dms.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InviteUsersResponse {
    @JsonProperty("successfully_invited_emails")
    private List<String> successfullyInvitedEmails;

    @JsonProperty("already_in_org_emails")
    private List<String> alreadyInOrgEmails;
}
