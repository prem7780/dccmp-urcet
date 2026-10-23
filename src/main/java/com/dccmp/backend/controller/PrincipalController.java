package com.dccmp.backend.controller;

import com.dccmp.backend.dto.Records;
import com.dccmp.backend.entity.*;
import com.dccmp.backend.repository.*;
import com.dccmp.backend.service.*;
import com.dccmp.backend.security.Access;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.stream.*;

@RestController
@RequestMapping("/api")
public class PrincipalController {
    private final Access access;
    private final StaffRepository staff;
    private final ApplicationRepository apps;
    private final WorkflowService flow;
    private final DocumentService documents;

    public PrincipalController(Access access, StaffRepository staff, ApplicationRepository apps, WorkflowService flow, DocumentService documents) {
        this.access = access;
        this.staff = staff;
        this.apps = apps;
        this.flow = flow;
        this.documents = documents;
    }

    @GetMapping("/principal/pending")
    public List<Records.ApplicationView> principalPending(HttpServletRequest req) {
        access.currentStaff(req, staff, "PRINCIPAL");
        return apps.findByStatusOrderBySubmittedAtDesc("PENDING_PRINCIPAL").stream().map(flow::view).toList();
    }

    @PatchMapping("/principal/applications/{id}/approve")
    public Records.ApplicationView principalApprove(@PathVariable long id, @RequestBody(required = false) Records.DecisionInput in, HttpServletRequest req) {
        Staff s = access.currentStaff(req, staff, "PRINCIPAL");
        Application a = flow.decidePrincipal(s, id, true, in == null ? "" : in.remarks());
        documents.issue(a.id);
        documents.sendForm(a.id);
        return flow.view(a);
    }

    @PatchMapping("/principal/applications/{id}/reject")
    public Records.ApplicationView principalReject(@PathVariable long id, @RequestBody Records.DecisionInput in, HttpServletRequest req) {
        return flow.view(flow.decidePrincipal(access.currentStaff(req, staff, "PRINCIPAL"), id, false, in.remarks()));
    }
}
